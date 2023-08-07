lazy val root = (project in file("."))
  .settings(scalaVersion := "3.3.0")
  .aggregate(util, core)
  .enablePlugins(FlakyPlugin)

lazy val util = (project in file("util"))
  .settings(
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.16",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % "test"
  )

lazy val core = (project in file("core"))
  .settings(
    libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.16",
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.16" % "test"
  )
