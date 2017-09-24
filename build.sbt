lazy val `sbt-flaky` =
  (project in file("."))
    .enablePlugins(GitVersioning, GitBranchPrompt, ScriptedPlugin)
    .settings(
      libraryDependencies ++= Seq(
        "org.apache.commons" % "commons-vfs2" % "2.1",
        "com.lihaoyi" %% "scalatags" % "0.6.5",
        "com.typesafe.sbt" % "sbt-git" % "0.9.3",
        "io.circe" %% "circe-core" % "0.8.0",
        "io.circe" %% "circe-generic" % "0.8.0",
        "io.circe" %% "circe-parser" % "0.8.0",
        "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value,
        "org.scalatest" %% "scalatest" % "3.0.1" % "test"
      ),
      addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.9.3"),
      sbtPlugin := true
    )


resolvers += "Sonatype OSS" at "https://oss.sonatype.org/content/repositories/releases"

git.useGitDescribe := true

organization := "pl.otrebski"

name := "sbt-flaky"

version := "0.14-SNAPSHOT"

scalaVersion := "2.12.3"


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

