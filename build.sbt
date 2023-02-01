lazy val `sbt-flaky` =
  (project in file("."))
    .enablePlugins(ScriptedPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "org.apache.commons" % "commons-vfs2" % "2.1",
        "com.lihaoyi" %% "scalatags" % "0.12.0",
        "org.eclipse.jgit" % "org.eclipse.jgit" % "6.4.0.202211300538-r",
        //"com.github.sbt" % "sbt-git" % "1.0.2",
        "io.circe" %% "circe-core" % "0.14.3",
        "io.circe" %% "circe-generic" % "0.14.3",
        "io.circe" %% "circe-parser" % "0.14.3",
        "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
        "org.scalatest" %% "scalatest" % "3.0.1" % "test"
      ),
      // addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "2.0.1"),
      sbtPlugin := true
    )

// git.useGitDescribe := true

organization := "pl.otrebski"

name := "sbt-flaky"

version := "0.15-SNAPSHOT"

scalaVersion := "2.12.17"


publishMavenStyle := false

bintrayOrganization := Some("kotrebski")

bintrayPackageLabels := Seq("sbt", "flaky-test")

bintrayRepository := "sbt-plugins"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

initialCommands in console :=
  """
    | println("Hello from console")
    | import java.io.File
    | import flaky._
    | val flakyReportDirSuccessful: File = new File("./src/test/resources/flakyTestRuns/successful/target/flaky-report/")
    | val successfulReport: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2"), flakyReportDirSuccessful)
    | val flakyReportDirWithFailures: File = new File("./src/test/resources/flakyTestRuns/withFailures/target/flaky-report/")
    | val failedReport: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2", "3"), flakyReportDirWithFailures)
    | val flakyReportDirAllFailures: File = new File("./src/test/resources/flakyTestRuns/allFailures/target/flaky-test-reports/")
    | val flakyReportAllFailures: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2", "3", "4", "5"), flakyReportDirAllFailures)
    |""".stripMargin

