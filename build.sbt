name := "contact-scrapper"

version := "0.1"

scalaVersion := "2.13.2"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-stream" % "2.6.5" ::
  "com.lihaoyi" %% "requests" % "0.5.1" ::
  "com.lihaoyi" %% "upickle" % "0.9.5" ::
  "eu.timepit" %% "refined" % "0.9.14" ::
  "io.scalaland" %% "chimney" % "0.5.1" ::
  "com.nrinaudo" %% "kantan.csv" % "0.6.0" ::
  Nil

val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= List(
  "org.mdedetrich" %% "akka-stream-circe" % "0.6.0",
  "org.mdedetrich" %% "akka-http-circe" % "0.6.0"
)
