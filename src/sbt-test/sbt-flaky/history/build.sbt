name := "history"

scalaVersion := "2.12.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0" % "test"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7" % "test"

lazy val library = (project in file("."))
  .enablePlugins(FlakyPlugin)
  .settings(flakyHistoryDir := Some(new File("history")))
