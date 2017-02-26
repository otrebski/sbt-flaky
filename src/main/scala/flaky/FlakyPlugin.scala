package flaky

import sbt.{AutoPlugin, Keys, SettingKey, settingKey}

object FlakyPlugin extends AutoPlugin {

  object autoImport {
    val flakySlackHook: SettingKey[Option[String]] = settingKey[Option[String]]("Slack web hook used to notify about test result")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    Keys.commands += FlakyCommand.flaky,
    flakySlackHook := None
  )

}
