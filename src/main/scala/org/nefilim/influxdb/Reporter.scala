package org.nefilim.influxdb

import java.net.InetSocketAddress
import java.text.DecimalFormat

import akka.actor.{Actor, ActorLogging, Props}
import com.codahale.metrics.{Meter, MetricRegistry, Timer}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.implicitConversions

/**
 * Created by peter on 11/16/14.
 */
case class InfluxEntry(name: String, columns: Seq[String], points: Option[List[List[BigDecimal]]])

object Reporter {
  object Columns {
    val TimerColumns = List(
      "time",
      "count",
      "min",
      "max",
      "mean",
      "std-dev",
      "50th-percentile",
      "75th-percentile",
      "95th-percentile",
      "99th-percentile",
      "999th-percentile",
      "one-minute",
      "five-minute",
      "fifteen-minute",
      "mean-rate"
    )

    val MeterColumns = List(
      "time",
      "count",
      "one-minute",
      "five-minute",
      "fifteen-minute",
      "mean-rate"
    )
  }

  object Conversion {
    import org.nefilim.influxdb.Reporter.Columns._
    import spray.json._
    object InfluxJsonProtocol extends DefaultJsonProtocol {
      implicit val influxEntryFormat = jsonFormat3(InfluxEntry)
    }
    import org.nefilim.influxdb.Reporter.Conversion.InfluxJsonProtocol._

    def round(d: Double)(implicit formatter: DecimalFormat): BigDecimal = BigDecimal(formatter.format(d))
    implicit def long2BigDecimal(l: Long) = BigDecimal(l)

    def timer2Json(name: String, timer: Timer)(implicit formatter: DecimalFormat): String = {
      val snapshot = timer.getSnapshot
      val point = List(List[BigDecimal](
        System.currentTimeMillis()/1000,
        snapshot.size,
        snapshot.getMin,
        snapshot.getMax,
        round(snapshot.getMean),
        round(snapshot.getStdDev),
        round(snapshot.getMedian),
        round(snapshot.get75thPercentile()),
        round(snapshot.get95thPercentile()),
        round(snapshot.get99thPercentile()),
        round(snapshot.get999thPercentile()),
        round(timer.getOneMinuteRate),
        round(timer.getFiveMinuteRate),
        round(timer.getFifteenMinuteRate),
        round(timer.getMeanRate)
      ))
      assert (TimerColumns.length == point(0).size)
      s"[${InfluxEntry(name, TimerColumns, Some(point)).toJson.compactPrint}]"
    }

    def meter2Json(name: String, meter: Meter)(implicit formatter: DecimalFormat): String = {
      val point = List(List[BigDecimal](
        System.currentTimeMillis()/1000,
        meter.getCount,
        round(meter.getOneMinuteRate),
        round(meter.getFiveMinuteRate),
        round(meter.getFifteenMinuteRate),
        round(meter.getMeanRate)
      ))
      assert (MeterColumns.length == point(0).size)
      s"[${InfluxEntry(name, MeterColumns, Some(point)).toJson.compactPrint}]"
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
  
  context.system.scheduler.schedule(reportingInterval, reportingInterval, self, Report)
  val udpSender = context.actorOf(Props(classOf[UDPSender], new InetSocketAddress(influxDBHost, influxDBPort)))

  // not thread safe
  implicit val formatter = new DecimalFormat("#.##")

  def receive: Receive = {
    case Report =>
      log.debug("waking up to report")
      val meters = registry.getMeters
      meters.keySet.asScala.foreach { k =>
        udpSender ! meter2Json(k, meters.get(k))
      }

      val timers = registry.getTimers
      timers.keySet.asScala.foreach { k =>
        udpSender ! timer2Json(k, timers.get(k))
      }
  }
}
