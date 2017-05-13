organization := "pl.otrebski"

name := "sbt-flaky"

version := "0.9-SNAPSHOT"

//scalaVersion := "2.11.8"

sbtPlugin := true

javaVersionPrefix in javaVersionCheck := Some("1.8") //Should I compile with specific version?

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false

libraryDependencies += "org.apache.commons" % "commons-vfs2" % "2.1"

libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.6.5"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

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

