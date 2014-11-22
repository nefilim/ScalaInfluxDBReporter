package org.nefilim.influxdb

import java.net.InetSocketAddress
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props}
import com.codahale.metrics.{Meter, MetricRegistry, Timer}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.implicitConversions

/**
 * Created by peter on 11/16/14.
 */
case class InfluxEntry(name: String, time_precision: String = "ms", columns: Seq[String], points: Option[List[List[BigDecimal]]])

object Reporter {
  val MilliSeconds = "ms"

  object Columns {
    val TimerColumns = List(
      "time",
      "tcount",
      "tmin",
      "tmax",
      "tmean",
      "std_dev",
      "50th_percentile",
      "75th_percentile",
      "95th_percentile",
      "99th_percentile",
      "999th_percentile",
      "one_minute",
      "five_minute",
      "fifteen_minute",
      "mean_rate"
    )

    val MeterColumns = List(
      "time",
      "mcount",
      "one_minute",
      "five_minute",
      "fifteen_minute",
      "mean_rate",
      "mcount_delta"
    )

    // make configurable?
    val RateFactor = TimeUnit.SECONDS.toSeconds(1)
    val DurationFactor = 1.0 / TimeUnit.MILLISECONDS.toNanos(1)

    def convertDuration(duration: Double): Double = duration * DurationFactor
    def convertRate(rate: Double): Double = rate * RateFactor
  }

  // NOT THREAD SAFE should only be accessed from the actor
  // TODO safer to move this inside the actor? makes for ugly interface
  private var previousMeterCount = Map.empty[String, Long] // no snapshot available :(

  private[influxdb] object Conversion {
    import org.nefilim.influxdb.Reporter.Columns._
    import spray.json._

    object InfluxJsonProtocol extends DefaultJsonProtocol {
      implicit val influxEntryFormat = jsonFormat4(InfluxEntry)
    }
    import org.nefilim.influxdb.Reporter.Conversion.InfluxJsonProtocol._

    def round(d: Double)(implicit formatter: DecimalFormat): BigDecimal = BigDecimal(formatter.format(d))
    implicit def long2BigDecimal(l: Long) = BigDecimal(l)

    def timer2Json(name: String, timer: Timer, timestamp: Long)(implicit formatter: DecimalFormat): String = {
      val snapshot = timer.getSnapshot
      val point = List(List[BigDecimal](
        timestamp,
        snapshot.size,
        round(convertDuration(snapshot.getMin)),
        round(convertDuration(snapshot.getMax)),
        round(convertDuration(snapshot.getMean)),
        round(convertDuration(snapshot.getStdDev)),
        round(convertDuration(snapshot.getMedian)),
        round(convertDuration(snapshot.get75thPercentile())),
        round(convertDuration(snapshot.get95thPercentile())),
        round(convertDuration(snapshot.get99thPercentile())),
        round(convertDuration(snapshot.get999thPercentile())),
        round(convertRate(timer.getOneMinuteRate)),
        round(convertRate(timer.getFiveMinuteRate)),
        round(convertRate(timer.getFifteenMinuteRate)),
        round(convertRate(timer.getMeanRate))
      ))
      assert (TimerColumns.length == point(0).size)
      s"[${InfluxEntry(name, MilliSeconds, TimerColumns, Some(point)).toJson.compactPrint}]"
    }

    def meter2Json(name: String, meter: Meter, timestamp: Long)(implicit formatter: DecimalFormat): String = {
      val currentCount = meter.getCount
      val point = List(List[BigDecimal](
        timestamp,
        currentCount,
        round(convertRate(meter.getOneMinuteRate)),
        round(convertRate(meter.getFiveMinuteRate)),
        round(convertRate(meter.getFifteenMinuteRate)),
        round(convertRate(meter.getMeanRate)),
        currentCount - previousMeterCount.get(name).getOrElse(0L)
      ))
      assert (MeterColumns.length == point(0).size)
      previousMeterCount = previousMeterCount + (name -> currentCount)
      s"[${InfluxEntry(name, MilliSeconds, MeterColumns, Some(point)).toJson.compactPrint}]"
    }
  }

  case object Report
}

import org.nefilim.influxdb.Reporter.Conversion._
import org.nefilim.influxdb.Reporter.Report

class Reporter(reportingInterval: FiniteDuration, registry: MetricRegistry, influxDBHost: String, influxDBPort: Int)
  extends Actor
  with ActorLogging {

  import context.dispatcher

  log.info("starting InfluxDB Reporter with host {}:{} at interval {}", influxDBHost, influxDBPort, reportingInterval)
  
  context.system.scheduler.schedule(reportingInterval, reportingInterval, self, Report)
  val udpSender = context.actorOf(Props(classOf[UDPSender], new InetSocketAddress(influxDBHost, influxDBPort)))

  // not thread safe
  implicit val formatter = new DecimalFormat("#.##")

  def receive: Receive = {
    case Report =>
      log.debug("waking up to report")
      // on the fence about synchronizing timestamps, it seems less accurate ultimately but probably useful to influx/grafana
      val timestamp = System.currentTimeMillis()/1000
      val meters = registry.getMeters
      meters.keySet.asScala.foreach { k =>
        udpSender ! meter2Json(k, meters.get(k), timestamp)
      }

      val timers = registry.getTimers
      timers.keySet.asScala.foreach { k =>
        udpSender ! timer2Json(k, timers.get(k), timestamp)
      }
  }
}
