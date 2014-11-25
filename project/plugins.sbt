scalacOptions ++= Seq( "-unchecked", "-deprecation" )

credentials += Credentials(Path.userHome / ".sbt" / ".credentials")

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "0.8.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-pgp" % "0.8.3")
