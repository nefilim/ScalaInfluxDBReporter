package org.nefilim.influxdb

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestKit
import com.typesafe.scalalogging.slf4j.LazyLogging
import nl.grons.metrics.scala.MetricName
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.Random
/**
 * Created by peter on 11/16/14.
 */
object MetricsRegistryHolder extends LazyLogging {
  logger.info("creating metricsRegistry")
  val metricRegistry = new com.codahale.metrics.MetricRegistry()
}

trait Instrumented extends nl.grons.metrics.scala.InstrumentedBuilder {
  val metricRegistry = MetricsRegistryHolder.metricRegistry
}

class InfluxDBReporterSpec
  extends TestKit(ActorSystem("InfluxDBReporterSpec"))
  with WordSpecLike
  with BeforeAndAfterAll
  with Instrumented {

  val random = Random
  override lazy val metricBaseName = MetricName("test")
  val meter = metrics.meter("mymeter")

  "InfluxDBReporter" should {
    "send out proper JSON over UDP" in {
      val reporter = system.actorOf(Reporter.props(metricRegistry, "host", 4444, 10.seconds))

      val total = 20*1000
      for (i <- 1 to total) {
        meter.mark()
        Thread.sleep(random.nextInt(30))
      }
    }
  }

  override protected def afterAll(): Unit = {
    system.shutdown()
  }
}
