ScalaInfluxDBReporter
=====================

A Scala (codahale) metrics reporter for InfluxDB over UDP using Akka

It currently only reports Timers and Meters as those are the ones I use but it would be trivial to add support for the rest. 

It would also be easy to add support for the HTTP based protocol but UDP seems preferable for metrics as it has fewer opportunity to impact the client process. 

Usage
=====

The reporter is modeled as an Akka actor, which limits integration possibilities (but why aren't you using Akka?:P). 

See src/test/scala/org/nefilim/influxdb/InfluxDBReporterSpec.scala for sample usage. 
