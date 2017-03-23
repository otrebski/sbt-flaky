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
      .groupBy(_.clazz)
      .map { kv =>
        val clazz = kv._1
        val list = kv._2
        val r = list
          .map(flaky => f":red_circle: *${flaky.failures * 100f / flaky.totalRun}%.2f%%* ${flaky.test} ")
          .mkString("\\n")
        s"*$clazz*:\\n$r"
      }.mkString("\\n")
    val summaryAttachment =
      s"""
         |{
         |  "fallback": "Flaky test result for $projectName",
         |  "color": "danger",
         |  "pretext": "Flaky test report for $projectName. Test were run ${flakyTestReport.testRuns.size} times",
         |  "author_name": "sbt-flaky",
         |  "title": "Flaky test result: $failedCount test failed of ${flaky.size} tests.\\nTest were running for $timeSpend [$timeSpendPerIteration/iteration]",
         |  "text": "$flakyText",
         |  "footer": "sbt-flaky",
         |  "ts": $timestamp
         |}
       """.stripMargin

    val flakyCases = flakyTestReport.groupFlakyCases()
    val failedAttachments: Iterable[String] = flakyCases.flatMap {
      case (test, flakyTestCases) =>
        flakyTestCases.map {
          fc =>
            val message = fc.message.map(_.escapeJson()).getOrElse("?")
            val runNames = fc.runNames.mkString(", ")
            val text =
              s"""| :poop: $test failed in following test runs: $runNames with message:
                  | $message
                  | ${fc.stacktrace}""".stripMargin
            s"""
               |{
               |  "fallback": "Flaky test report for $test",
               |  "color": "danger",
               |  "pretext": "Flaky test report for *$test*",
               |  "author_name": "sbt-flaky",
               |  "title": "Flaky test details for $test: ",
               |  "text": "${text.escapeJson()}",
               |  "footer": "sbt-flaky",
               |  "ts": $timestamp
               |}
           """.stripMargin
        }

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

  def send(webHook: String, jsonMsg: String, log: Logger, targetDir: File): Unit = {
    log.info("Sending report to slack")
    log.debug("Dumping slack msg to file")
    val file = new File(targetDir, "slack.json")
    new PrintWriter(file) {
      write(jsonMsg)
      close()
    }

    val send: Try[Unit] = Try {
      val url = new URL(webHook)
      val urlConnection = url.openConnection().asInstanceOf[HttpURLConnection]
      // Indicate that we want to write to the HTTP request body
      urlConnection.setDoOutput(true)
      urlConnection.setRequestMethod("POST")

      // Writing the post data to the HTTP request body
      log.debug(jsonMsg)
      val httpRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()))
      httpRequestBodyWriter.write(jsonMsg)
      httpRequestBodyWriter.close()

      val scanner = new Scanner(urlConnection.getInputStream())
      log.debug("Response from SLACK:")
      while (scanner.hasNextLine) {
        log.debug(s"Response from SLACK: ${scanner.nextLine()}")
      }
      scanner.close()
    }
    send match {
      case Success(_) => log.info("Notification successfully send to Slack")
      case Failure(e) => log.error(s"Can't send message to slack: ${e.getMessage}")
    }

  }


  class ToJsonString(val string: String) {
    def escapeJson(): String = {
      string
        .replace("\\","\\\\")
        .replace("\t","\\t")
        .replace("\"","\\\"")
        .replace("\n","\\n")
    }
  }

  implicit def stringToJsonString(s: String): ToJsonString = new ToJsonString(s)
}


