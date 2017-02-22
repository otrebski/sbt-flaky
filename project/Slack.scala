import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}
import java.util.Scanner

import sbt.Logger

import scala.collection.immutable.{Iterable, Seq}

object Slack {



  def render(flaky: List[FlakyTest]): String = {
    if (flaky.exists(_.failures > 0)) {
      renderFailed(flaky)
    } else {
      renderNoFailures(flaky)
    }
  }

  def renderNoFailures(flaky: List[FlakyTest]): String = {
    val timestamp = System.currentTimeMillis()
    val summaryAttachment =
      s"""
         |{
         |  "fallback": "Flaky test result",
         |  "color": "#36a64f",
         |  "pretext": "Flaky test report",
         |  "author_name": "sbt-flaky",
         |  "title": "Flaky test result",
         |  "text": "All tests are correct [${flaky.headOption.map(f => f.totalRun).getOrElse(0)} runs]",
         |  "footer": "sbt-flaky",
         |  "ts": $timestamp
         |}
       """.stripMargin
    summaryAttachment
  }

  //TODO escape values in JSON
  def renderFailed(flaky: List[FlakyTest]): String = {
    val failedCount = flaky.count(_.failures > 0)
    val flakyText = flaky
      .filter(_.failures > 0)
      .groupBy(_.clazz)
      .map { kv =>
        val clazz = kv._1
        val list = kv._2
        val r = list
          .map(flaky => s"* ${flaky.test} ${flaky.failures * 100 / flaky.totalRun}%")
          .mkString("\\n")
        s"$clazz:\\n$r"
      }.mkString("\\n")

    val timestamp = System.currentTimeMillis

    val summaryAttachment =
      s"""
         |{
         |  "fallback": "Flaky test result",
         |  "color": "danger",
         |  "pretext": "Flaky test report",
         |  "author_name": "sbt-flaky",
         |  "title": "Flaky test result: $failedCount on ${flaky.size}",
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
           |  "text": "${text.mkString("\\n")}",
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

  def send(hookId: String, jsonMsg: String,log: Logger): Unit = {
    log.info("Sending report")
    val url = new URL(s"https://hooks.slack.com/services/$hookId")
    val urlConnection = url.openConnection().asInstanceOf[HttpURLConnection]
    // Indicate that we want to write to the HTTP request body
    urlConnection.setDoOutput(true)
    urlConnection.setRequestMethod("POST")

    // Writing the post data to the HTTP request body
    val httpRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()))
    httpRequestBodyWriter.write(jsonMsg)
    httpRequestBodyWriter.close()

    val scanner = new Scanner(urlConnection.getInputStream())
    log.info("Response from SLACK:")
    while (scanner.hasNextLine) {
      log.info(s"Response from SLACK: ${scanner.nextLine()}")
    }
    scanner.close()
  }
}
