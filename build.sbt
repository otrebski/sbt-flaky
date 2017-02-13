import FlakyCommand._

name := "Flaky"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.6"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "com.typesafe.akka" % "akka-actor_2.11" % "2.4.17" % "test"

libraryDependencies += "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.17" % "test"

libraryDependencies += "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.5.0" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7" % "test"

resolvers += Resolver.typesafeRepo("releases")

lazy val root = (project in file(".")).
  settings(commands ++= Seq(flaky))
