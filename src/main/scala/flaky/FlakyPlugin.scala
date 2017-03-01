package flaky

import sbt._

object FlakyPlugin extends AutoPlugin {

  object autoImport {
    val flakySlackHook: SettingKey[Option[String]] = settingKey[Option[String]]("Slack web hook used to notify about test result")
    val flakyTask: SettingKey[Seq[TaskKey[Unit]]] = settingKey[Seq[TaskKey[Unit]]]("Tasks to run, by default test in Test")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    Keys.commands += FlakyCommand.flaky,
    flakySlackHook := None,
    flakyTask := Seq(Keys.test in Test)
  )

}
