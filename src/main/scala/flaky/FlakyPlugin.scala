package flaky

import sbt._

object FlakyPlugin extends AutoPlugin {

  override def requires: Plugins = {
    sbt.plugins.JvmPlugin
  }

  object autoImport {
    val flakySlackHook: SettingKey[Option[String]] = settingKey[Option[String]]("Slack web hook used to notify about test result")
    val flakyTask: SettingKey[Seq[TaskKey[Unit]]] = settingKey[Seq[TaskKey[Unit]]]("Tasks to run, by default test in Test")
    val flakyReportsDir: SettingKey[String] = settingKey[String]("Name of folder in target dir to store test reports and additional files")
    val flakyReportsHtmlSinglePage: SettingKey[String] = settingKey[String]("Name of folder in target dir to store test reports and additional files")
    val flakyAdditionalFiles: SettingKey[List[File]] = settingKey[List[File]]("List of additional files to backup after test run (for example log files)")
    val flakyLogLevelInTask: SettingKey[sbt.Level.Value] = settingKey[sbt.Level.Value]("SBT logger level for tasks")
    val flakyHistoryDir: SettingKey[Option[File]] = settingKey[Option[File]]("Dir to keep history, for calculating trends")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    Keys.commands += FlakyCommand.flaky,
    flakySlackHook := None,
    flakyTask := Seq(Keys.test in sbt.Test),
    flakyReportsDir := "flaky-test-reports",
    flakyReportsHtmlSinglePage := "flaky-test-reports-html",
    flakyAdditionalFiles := List.empty[File],
    flakyLogLevelInTask := sbt.Level.Info,
    flakyHistoryDir := None
  )

}
