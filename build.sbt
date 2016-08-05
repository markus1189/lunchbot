name := "lunchbot"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

val akkaVersion: String = "2.4.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-core" % akkaVersion,
  "com.typesafe.akka" %% "akka-http-experimental" % akkaVersion,
  "com.typesafe" % "config" % "1.3.0",
  "de.heikoseeberger" %% "akka-http-circe" % "1.7.0",
  "io.circe" %% "circe-parse" % "0.2.1",
  "io.circe" %% "circe-generic" % "0.5.0-M2",
  "org.typelevel" %% "cats" % "0.6.0",
  "org.typelevel" %% "cats-free" % "0.6.0",

  "org.scalatest" %% "scalatest" % "3.0.0-M16-SNAP6" % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
)
