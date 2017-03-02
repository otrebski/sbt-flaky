enablePlugins(FlakyPlugin)

name := "sendToSlack"

scalaVersion := "2.11.8"

flakySlackHook := Some("http://127.0.0.1/hook/id/x")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7" % "test"
