package flaky

import java.io.{BufferedWriter, File, OutputStreamWriter, PrintWriter}
import java.net.{HttpURLConnection, URL}
import java.util.Scanner

import sbt.Logger

import scala.collection.immutable.{Iterable, Seq}
import scala.util.{Failure, Success, Try}

object Slack {

  val escapedBackslash = """\\\""""

  def render(projectName: String, flaky: List[FlakyTest]): String = {
    if (flaky.exists(_.failures > 0)) {
      renderFailed(projectName, flaky)
    } else {
      renderNoFailures(projectName, flaky)
    }
  }

  def renderNoFailures(projectName: String, flaky: List[FlakyTest]): String = {
    //can use lib for message formatting https://github.com/gilbertw1/slack-scala-client/blob/master/src/main/scala/slack/models/package.scala
    val timestamp = System.currentTimeMillis()
    val summaryAttachment =
      s"""
         |{
         |  "fallback": "Flaky test result for $projectName",
         |  "color": "#36a64f",
         |  "pretext": "Flaky test report for $projectName",
         |  "author_name": "sbt-flaky",
         |  "title": "Flaky test result",
         |  "text": "All tests are correct [${flaky.headOption.map(f => f.totalRun).getOrElse(0)} runs]",
         |  "footer": "sbt-flaky",
         |  "ts": $timestamp
         |}
       """.stripMargin
    summaryAttachment
  }

  def renderFailed(projectName: String, flaky: List[FlakyTest]): String = {
    val failedCount = flaky.count(_.failures > 0)
    val flakyText = flaky
      .filter(_.failures > 0)
      .groupBy(_.clazz)
      .map { kv =>
        val clazz = kv._1
        val list = kv._2
        val r = list
          .map(flaky => f"* ${flaky.test} ${flaky.failures * 100f / flaky.totalRun}%.2f%%")
          .mkString("\\n")
        s"$clazz:\\n$r"
      }.mkString("\\n")

    val timestamp = System.currentTimeMillis

    val summaryAttachment =
      s"""
         |{
         |  "fallback": "Flaky test result for $projectName",
         |  "color": "danger",
         |  "pretext": "Flaky test report for $projectName",
         |  "author_name": "sbt-flaky",
         |  "title": "Flaky test result: $failedCount of ${flaky.size}",
         |  "text": "$flakyText",
         |  "footer": "sbt-flaky",
         |  "ts": $timestamp
         |}
       """.stripMargin

    val failedAttachments: Iterable[String] = flaky
      .filter(_.failures > 0)
      .groupBy(t => s"${t.clazz}")
      .map { kv =>
        val clazzTestName = kv._1
        val list: Seq[FlakyTest] = kv._2

        val text: Iterable[String] = list.groupBy(_.test)
          .flatMap {
            case (test, l) =>
              l.map { ft =>
                val messagesOnFail = ft.failedRuns
                  .flatMap(tc => tc.failureDetails)
                  .map(fr => fr.message)
                  .groupBy(identity).mapValues(_.size)
                  .map((a) => s" * ${a._2} => ${a._1}")
                  .mkString("\\n")
                s"$test:\\n$messagesOnFail"
              }
          }

        s"""
           |{
           |  "fallback": "Flaky test result for $clazzTestName",
           |  "color": "danger",
           |  "pretext": "Report for $clazzTestName",
           |  "author_name": "sbt-flaky",
           |  "title": "Flaky test details for $clazzTestName: ",
           |  "text": "${text.mkString("\\n").replace("\"", escapedBackslash).replace("\n", "\\n").replace("\r", "\\r")}",
           |  "footer": "sbt-flaky",
           |  "ts": $timestamp
           |}
           """.stripMargin

      }

    val msg =
      s"""
   {
       "attachments": [
          $summaryAttachment,
          ${failedAttachments.mkString(",")}
       ]
   }
   """
    msg
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
}
