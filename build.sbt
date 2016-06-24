name := "lunchbot"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-core" % "2.4.7",
  "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7"
)

resolvers += Resolver.bintrayRepo("hseeberger", "maven")

libraryDependencies ++= List(
  "de.heikoseeberger" %% "akka-http-circe" % "1.7.0"
)
