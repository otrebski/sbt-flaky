package flaky.report

import scala.collection.immutable.Iterable
import scala.language.implicitConversions

import _root_.flaky.slack.model._
import flaky.json.{JsObject, JsonWriter}
import flaky.web._
import flaky.{FlakyCase, FlakyTestReport, TimeReport, web}

object SlackReport {

  object Icons {
    val link = ":link:"
    val failedClass = ":bangbang:"
    val detailForClass = ":poop:"
    val failCase = ":small_orange_diamond:"
  }

  def render(flakyTestReport: FlakyTestReport,
             htmlReportsUrl: Option[String] = None,
             details: Boolean = false): String = {

    val r: Message = if (flakyTestReport.flakyTests.exists(_.failures > 0)) {
      if (details) {
        renderFailed(flakyTestReport, htmlReportsUrl)
      } else {
        renderFailedShort(flakyTestReport, htmlReportsUrl)
      }
    } else {
      renderNoFailures(flakyTestReport)
    }

    import flaky.json.JsonSyntax._
    implicit val messageWriter: JsonWriter[Message] = (value: Message) => {
      import flaky.json.JsonWriterInstances._
      implicit val attachmentWriter: JsonWriter[Attachment] = (a: Attachment) => JsObject(Map(
        "falback" -> a.fallback.toJson,
        "callback_id" -> a.callback_id.toJson,
        "color" -> a.color.toJson,
        "pretext" -> a.pretext.toJson,
        "author_name" -> a.author_name.toJson,
        "author_link" -> a.author_link.toJson,
        "author_icon" -> a.author_icon.toJson,
        "title" -> a.title.toJson,
        "title_link" -> a.title_link.toJson,
        "text" -> a.text.toJson,
        "image_url" -> a.image_url.toJson,
        "thumb_url" -> a.thumb_url.toJson,
        "footer" -> a.footer.toJson,
        "ts" -> a.ts.toJson,
        "mrkdwn_in" -> a.mrkdwn_in.toJson
      ))
      implicit val attachmentList: JsonWriter[Seq[Attachment]] = listWriter(attachmentWriter)
      val attachmentJson = JsObject(Map(
        "text" -> value.text.toJson,
        "attachments" -> value.attachments.toJson
      ))
      attachmentJson
    }

    val result = r.toJson
    result.compactPrint()
  }

  def renderNoFailures(flakyTestReport: FlakyTestReport): Message = {
    val timestamp = flakyTestReport.timeDetails.start
    val projectName = flakyTestReport.projectName
    val flaky = flakyTestReport.flakyTests
    val duration = flakyTestReport.timeDetails.duration()
    val timeSpend = TimeReport.formatSeconds(duration / 1000)
    val timeSpendPerIteration = if (flakyTestReport.testRuns.nonEmpty) {
      TimeReport.formatSeconds((duration / flakyTestReport.testRuns.size) / 1000)
    } else {
      "?"
    }

    val summary = Attachment(
      fallback = s"Flaky test result for $projectName",
      color = "#36a64f",
      pretext = s"Flaky test report for $projectName",
      author_name = "sbt-flaky",
      title = "Flaky test result",
      text = s"All tests are correct [${
        flaky.headOption.map(f => f.totalRun)
          .getOrElse(0)
      } runs]\nTest were running for $timeSpend [$timeSpendPerIteration/iteration]",
      footer = "sbt-flaky",
      ts = timestamp
    )
    Message(attachments = List(summary))
  }

  def renderFailed(flakyTestReport: FlakyTestReport, htmlReportsUrl: Option[String]): Message = {
    val timestamp = flakyTestReport.timeDetails.start
    val projectName = flakyTestReport.projectName
    val flaky = flakyTestReport.flakyTests
    val failedCount = flaky.count(_.failures > 0)
    val duration = flakyTestReport.timeDetails.duration()
    val timeSpend = TimeReport.formatSeconds(duration / 1000)
    val timeSpendPerIteration = TimeReport.formatSeconds((duration / flakyTestReport.testRuns.size) / 1000)
    val htmlReportLink = htmlReportsUrl.map(h => s"<$h/index.html|HTML report> ${Icons.link}")

    val flakyText = flaky
      .filter(_.failures > 0)
      .groupBy(_.test.clazz)
      .map { kv =>
        val clazz = kv._1
        val list = kv._2
        val r = list
          .sortBy(_.failures())
          .map { flaky =>
            val link = htmlReportsUrl
              .map(l => if (l.endsWith("/")) l else l + "/")
              .map(host => s"<$host/${linkToSingleTest(flaky.test)}|${Icons.link}>")
            f"`${100 - flaky.failures * 100f / flaky.totalRun}%.2f%%` `${flaky.test.test}` ${link.getOrElse("")}"
          }
          .mkString("\n")
        s"${Icons.failedClass} *$clazz:*\n$r"
      }.mkString("Success rate for tests:\n", "\n", "")

    val attachment = Attachment(
      fallback = "Flaky test result for $projectName",
      color = "danger",
      pretext = s"Flaky test report for $projectName. Test were run ${flakyTestReport.testRuns.size} times",
      author_name = "sbt-flaky",
      title =
        s"""$failedCount of ${flaky.size} test cases are flaky.
           |Build success probability is ${flakyTestReport.successProbabilityPercent()}.
           |Test were running for $timeSpend [$timeSpendPerIteration/iteration]
           |${htmlReportLink.getOrElse("")}""".stripMargin,
      text = flakyText,
      footer = "sbt-flaky",
      mrkdwn_in = Seq("text")
    )

    val flakyCases: Map[String, List[FlakyCase]] = flakyTestReport.groupFlakyCases()
    val failedAttachments: Iterable[Attachment] = flakyCases.map {
      case (testClass, flakyTestCases) =>
        val flakyTestsDescription: String = flakyTestCases
          .sortBy(_.runNames.size)
          .map {
            fc =>
              val test = fc.test
              val message = fc.message.getOrElse("?")
              val text =
                s"""|  ${Icons.failCase}[${fc.runNames.size} times] ${test.test}
                    |  Message: $message
                    |  ${fc.stacktrace}""".stripMargin
              text
          }.mkString("\n")

        val link = htmlReportsUrl
          .map(l => if (l.endsWith("/")) l else l + "/")
          .map(host => s"<$host/${web.linkToSingleTestClass(testClass)}|${Icons.link}>")
        Attachment(
          fallback = s"Flaky test report for $testClass",
          color = "danger",
          title = s"${Icons.failedClass} Details for $testClass: ${link.getOrElse("")}",
          text = flakyTestsDescription,
          ts = timestamp
        )

    }
    Message(attachments = attachment :: failedAttachments.toList)
  }

  def renderFailedShort(flakyTestReport: FlakyTestReport, htmlReportUrl: Option[String]): Message = {
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
          .map { flaky =>
            import _root_.flaky.web._
            val link = htmlReportUrl
              .map(l => if (l.endsWith("/")) l else l + "/")
              .map(host => s"<$host/${linkToSingleTest(flaky.test)}|${Icons.link}>")
            f" `${100 - (flaky.failures * 100f) / flaky.totalRun}%.2f%%` `${flaky.test.test}` ${link.getOrElse("")}"
          }
          .mkString("\n")

        s"${Icons.failedClass} *$clazz*:\n$r"
      }.mkString("Success rate for tests:\n", "\n", "")

    val htmlReportLink = htmlReportUrl.map(h => s"<$h/index.html|HTML report> ${Icons.link}")

    val attachment = Attachment(
      fallback = s"Flaky test result for $projectName",
      color = "danger",
      pretext = s"Flaky test report for $projectName. Test were run ${flakyTestReport.testRuns.size} times",
      author_name = "sbt-flaky",
      title =
        f"""$failedCount of ${flaky.size} test cases are flaky.
           |Build success probability is ${flakyTestReport.successProbabilityPercent()}%.2f%%.
           |Test were running for $timeSpend [$timeSpendPerIteration/iteration]
           |${htmlReportLink.getOrElse("")}""".stripMargin,
      text = flakyText,
      footer = "sbt-flaky",
      mrkdwn_in = Seq("text"),
      ts = timestamp
    )
    Message(attachments = List(attachment))
  }

}


