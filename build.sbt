name := "spacerock"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.0"

libraryDependencies ++= Seq(
   "com.netflix.astyanax" % "astyanax" % "1.56.48",
   "org.scaldi" %% "scaldi-play" % "0.3.3"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
