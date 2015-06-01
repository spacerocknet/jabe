name := "spacerock"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
   "org.scaldi" %% "scaldi-play" % "0.3.3",
   "com.datastax.cassandra" % "cassandra-driver-core" % "2.0.10",
   "redis.clients" % "jedis" % "2.6.0",
   "junit" % "junit" % "4.8.1" % "test"
)

testOptions += Tests.Argument(TestFrameworks.JUnit, "-v", "-q")
//javaOptions in Test += "-Dlogger.file=conf/test-logger.xml"

playScalaSettings
