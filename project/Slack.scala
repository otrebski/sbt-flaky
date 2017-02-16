import java.io.{BufferedWriter, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}
import java.util.Scanner

import scala.collection.immutable.{Iterable, Seq}

object Slack {

  //TODO escape values in JSON
  def render(flaky: List[FlakyTest]): String = {
    val failedCount = flaky.count(_.failures > 0)
    val flakyText = flaky
      .filter(_.failures > 0)
      .groupBy(_.clazz)
      .map { kv =>
        val clazz = kv._1
        val list = kv._2
        val r = list
          .map(flaky => s"- ${flaky.test} ${flaky.failures * 100 / flaky.totalRun}%")
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
         |  "title": "Flaky test result: $failedCount",
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
           .flatMap{
             case (test, l) =>
               l.map{ ft =>
                 val messagesOnFail = ft.failedRuns
                   .flatMap(tc => tc.failureDetails)
                   .map(fr => fr.message)
                   .groupBy(identity).mapValues(_.size)
                   .map((a) => s" - ${a._2} => ${a._1}")
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

  def send(hookId: String, jsonMsg: String): Unit = {
    println("Sending report")
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
    print("Response from SLACK:")
    while(scanner.hasNextLine) {
      println(scanner.nextLine())
    }
    scanner.close()
  }
}
