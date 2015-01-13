name := "spacerock"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.0"

libraryDependencies ++= Seq(
   "com.netflix.astyanax" % "astyanax" % "1.56.48",
   "org.scaldi" %% "scaldi-play" % "0.3.3",
   "com.datastax.cassandra" % "cassandra-driver-core" % "2.1.4"
)

playScalaSettings
