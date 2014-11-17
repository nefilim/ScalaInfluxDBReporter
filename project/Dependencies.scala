import sbt._

object Dependencies {

  object V {
    val scalaTest = "2.2.2"
    val slf4j = "1.7.7"
    val logback = "1.1.2"

    val akka = "2.3.7"
    val spray = "1.3.2-20140909"
    val scalaLogging = "2.1.2"

    val sprayJson = "1.3.1"
    val json4s = "3.2.11"
    val metricsScala = "3.3.0"
  }

  object Libraries {
    val scalaTest= "org.scalatest"           %% "scalatest"                % V.scalaTest % "test"
    val scalaLogging="com.typesafe.scala-logging" % "scala-logging-api_2.10" % V.scalaLogging
    val scalaLoggingSlf4j="com.typesafe.scala-logging" % "scala-logging-slf4j_2.10" % V.scalaLogging
    val logback = "ch.qos.logback"           % "logback-classic"          % V.logback % "test"

    val sprayClient = "io.spray"             %% "spray-client"		% V.spray
    val sprayRouting = "io.spray"            %% "spray-routing"  	% V.spray
    val sprayTestKit = "io.spray"            %% "spray-testkit"  	% V.spray % "test"
    val sprayJson = "io.spray" %%  "spray-json" % V.sprayJson

    val json4sNative = "org.json4s"          %% "json4s-native"		% V.json4s
    val json4sJackon = "org.json4s"          %% "json4s-jackson"	% V.json4s
    val json4sExtensions = "org.json4s"      %% "json4s-ext"		% V.json4s

    val akka = "com.typesafe.akka"           %% "akka-actor"		% V.akka
    val akkaTestKit = "com.typesafe.akka"    %% "akka-testkit"  % V.akka % "test"

    val metricsScala = "nl.grons"            %% "metrics-scala" % V.metricsScala
  }

  import Libraries._ 

  val influxDBReporter = Seq(scalaTest, scalaLoggingSlf4j, logback, sprayClient, sprayRouting, sprayJson, akka, sprayTestKit, metricsScala)
}
