import scala.collection.immutable.{Iterable, Seq}

object Slack {

  //TODO escpe values in JSON
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
      .groupBy(t => s"${t.clazz} ${t.test}")
      .flatMap { kv =>
        val clazzTestName = kv._1
        val list: Seq[FlakyTest] = kv._2
        list.map { ft =>
          val clazz = ft.clazz
          val test = ft.test
          val messagesOnFail = ft.failedRuns
            .flatMap(tc => tc.failureDetails)
            .map(fr => fr.message)
            .groupBy(identity).mapValues(_.size)
            .map((a) => s"${a._2} => ${a._1}")
            .mkString("\\n")
          s"""
             |{
             |  "fallback": "Flaky test result for $clazz: $test",
             |  "color": "danger",
             |  "pretext": "Report for $clazzTestName",
             |  "author_name": "sbt-flaky",
             |  "title": "Flaky test details for $clazz: $test",
             |  "text": "$messagesOnFail",
             |  "footer": "sbt-flaky",
             |  "ts": $timestamp
             |}
           """.stripMargin
        }
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
}
