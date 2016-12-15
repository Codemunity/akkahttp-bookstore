name := "AkkaHTTPBookstore"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-core" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-testkit" % "2.4.3",
  "com.pauldijou" %% "jwt-core" % "0.8.0",
  "org.scalatest" % "scalatest_2.11" % "3.0.1" % "test",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.1.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.flywaydb" % "flyway-core" % "3.2.1"
)