package flaky

import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.Scanner

import sbt.Logger

import scala.collection.immutable.Iterable
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object Slack {

  val escapedBackslash = """\\\""""

  def render(flakyTestReport: FlakyTestReport): String = {
    if (flakyTestReport.flakyTests.exists(_.failures > 0)) {
      renderFailed(flakyTestReport)
    } else {
      renderNoFailures(flakyTestReport)
    }
  }

  def renderNoFailures(flakyTestReport: FlakyTestReport): String = {
    //can use lib for message formatting https://github.com/gilbertw1/slack-scala-client/blob/master/src/main/scala/slack/models/package.scala
    val timestamp = flakyTestReport.timeDetails.start
    val projectName = flakyTestReport.projectName
    val flaky = flakyTestReport.flakyTests
    val duration = flakyTestReport.timeDetails.duration()
    val timeSpend = TimeReport.formatSeconds(duration / 1000)
    val timeSpendPerIteration = TimeReport.formatSeconds((duration / flakyTestReport.testRuns.size) / 1000)

    val summaryAttachment =
      s"""
         |{
         |  "fallback": "Flaky test result for $projectName",
         |  "color": "#36a64f",
         |  "pretext": "Flaky test report for $projectName",
         |  "author_name": "sbt-flaky",
         |  "title": "Flaky test result",
         |  "text": "All tests are correct [${flaky.headOption.map(f => f.totalRun).getOrElse(0)} runs]\\nTest were running for $timeSpend [$timeSpendPerIteration/iteration]",
         |  "footer": "sbt-flaky",
         |  "ts": $timestamp
         |}
       """.stripMargin
    summaryAttachment
  }

  def renderFailed(flakyTestReport: FlakyTestReport): String = {
    val timestamp = flakyTestReport.timeDetails.start
    val projectName = flakyTestReport.projectName
    val flaky = flakyTestReport.flakyTests
    val failedCount = flaky.count(_.failures > 0)
    val duration = flakyTestReport.timeDetails.duration()
    val timeSpend = TimeReport.formatSeconds(duration / 1000)
    val timeSpendPerIteration = TimeReport.formatSeconds((duration / flakyTestReport.testRuns.size) / 1000)


    val flakyText = flaky
      .filter(_.failures > 0)
      .groupBy(_.test.clazz)
      .map { kv =>
        val clazz = kv._1
        val list = kv._2
        val r = list
          .sortBy(_.failures())
          .map(flaky => f":red_circle: ${flaky.failures * 100f / flaky.totalRun}%.2f%% ${flaky.test} ")
          .mkString("\n")
        s"$clazz:\n$r"
      }.mkString("\n")

    val summaryAttachment =
      s"""
         |{
         |  "fallback": "Flaky test result for $projectName",
         |  "color": "danger",
         |  "pretext": "Flaky test report for $projectName. Test were run ${flakyTestReport.testRuns.size} times",
         |  "author_name": "sbt-flaky",
         |  "title": "Flaky test result: $failedCount test failed of ${flaky.size} tests.\\nTest were running for $timeSpend [$timeSpendPerIteration/iteration]",
         |  "text": "${flakyText.escapeJson()}",
         |  "footer": "sbt-flaky",
         |  "ts": $timestamp
         |}
       """.stripMargin

    val flakyCases: Map[String, List[FlakyCase]] = flakyTestReport.groupFlakyCases()
    val failedAttachments: Iterable[String] = flakyCases.map {
      case (testClass, flakyTestCases) =>
        val flakyTestsDescription: String = flakyTestCases
          .sortBy(_.runNames.size)
          .map {
            fc =>
              val test = fc.test
              val message = fc.message.map(_.escapeJson()).getOrElse("?")
              val runNames = fc.runNames.sorted.mkString(", ")
              val text =
                s"""| :small_orange_diamond:[${fc.runNames.size} times] $test
                    |  In following test runs: $runNames
                    |  Message: $message
                    | ${fc.stacktrace}""".stripMargin
              text
          }.mkString("\n")
          s"""
             |{
             |  "fallback": "Flaky test report for ${testClass.escapeJson()}",
             |  "color": "danger",
             |  "title": ":poop: Details for ${testClass.escapeJson()}: ",
             |  "text": "${flakyTestsDescription.escapeJson()}"
             |}
           """.stripMargin
    }

    s"""
       |{
       |    "attachments": [
       |       $summaryAttachment,
       |       ${failedAttachments.mkString(",")}
       |    ]
       |}
       |""".stripMargin
  }




  class ToJsonString(val string: String) {
    def escapeJson(): String = {
      string
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
    }
  }

  implicit def stringToJsonString(s: String): ToJsonString = new ToJsonString(s)
}


