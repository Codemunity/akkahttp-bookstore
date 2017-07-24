name := "AkkaHTTPBookstore"

version := "1.0"

scalaVersion := "2.11.8"

val akkaVersion = "2.5.3"
val akkaHttpVersion = "10.0.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.pauldijou" %% "jwt-core" % "0.8.0",
  "org.scalatest" % "scalatest_2.11" % "3.0.1" % "test",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.flywaydb" % "flyway-core" % "3.2.1",
  "com.github.t3hnar" %% "scala-bcrypt" % "3.0",
  "com.lihaoyi" %% "scalatags" % "0.6.5"
)