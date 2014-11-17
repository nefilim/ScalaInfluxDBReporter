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
  override lazy val metricBaseName = MetricName("Test")
  val timer = metrics.timer("mytimer")

  "InfluxDBReporter" should {
    "send out proper JSON over UDP" in {
      val interval = 5.seconds
      val reporter = system.actorOf(Props(classOf[Reporter], interval, metrics.registry, "10.0.26.199", 4444))

      for (i <- 1 to 20) {
        timer.time {
          Thread.sleep(random.nextInt(30))
        }
      }

      Thread.sleep((interval * 3).toMillis)
    }
  }

  override protected def afterAll(): Unit = {
    system.shutdown()
  }
}
