ScalaInfluxDBReporter
=====================

A Scala (codahale) metrics reporter for InfluxDB over UDP using Akka

It currently only reports Timers and Meters as those are the ones I use but it would be trivial to add support for the rest. 

It would also be easy to add support for the HTTP based protocol but UDP seems preferable for metrics as it has fewer opportunity to impact the client process. 

In addition to the reported Meter metric fields (time, mcount, one_minute, five_minute, fifteen_minute, mean_rate) ScalaInfluxDBReporter always maintains a little state, the mcount value of the previously reported Meter and adds another field: __mcount_delta__ that reports the change in Meter.count since the last report. 

It makes it easy to sum those in the influx time group by buckets to see the total per time period. 

Usage
=====

The reporter is modeled as an Akka actor, which limits integration possibilities (but why aren't you using Akka?:P). 

See src/test/scala/org/nefilim/influxdb/InfluxDBReporterSpec.scala for sample usage but basically it comes down to this one liner:

       val reporter = system.actorOf(Reporter.props(metricRegistry, "host", 4444, 10.seconds))

