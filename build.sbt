name := "contact-scrapper"

version := "0.1"

scalaVersion := "2.13.2"

mainClass in (Compile, run) := Some("ScrapperApp")

val akkaStreamCirce = "0.6.0"
val circeVersion = "0.13.0"

libraryDependencies ++=
  "com.typesafe.akka" %% "akka-stream" % "2.6.5" ::
  "com.lihaoyi" %% "requests" % "0.5.1" ::
  "com.lihaoyi" %% "upickle" % "0.9.5" ::
  "eu.timepit" %% "refined" % "0.9.14" ::
  "io.scalaland" %% "chimney" % "0.5.1" ::
  "com.nrinaudo" %% "kantan.csv" % "0.6.0" ::
  "com.github.ben-manes.caffeine" % "caffeine" % "2.8.2" ::
  "com.github.cb372" %% "scalacache-caffeine" % "0.28.0" ::
  "org.mdedetrich" %% "akka-stream-circe" % akkaStreamCirce ::
  "org.mdedetrich" %% "akka-http-circe" % akkaStreamCirce ::
  "io.circe" %% "circe-core" % circeVersion ::
  "io.circe" %% "circe-generic" % circeVersion ::
  "io.circe" %% "circe-parser" % circeVersion ::
  "org.tpolecat" %% "doobie-core" % "0.8.8" ::
  "org.tpolecat" %% "doobie-hikari" % "0.8.8" ::
  "mysql" % "mysql-connector-java" % "5.1.34" ::
  "org.scalatest" %% "scalatest" % "3.0.8" % Test ::
  "org.scalamock" %% "scalamock" % "4.3.0" % Test ::
  Nil
