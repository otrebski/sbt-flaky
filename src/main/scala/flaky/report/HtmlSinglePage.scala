package flaky.report

import flaky.{FlakyCase, FlakyTestReport, TimeReport}

import scalatags.Text
import scalatags.Text.all._

object HtmlSinglePage {


  def pageSource(flakyTestReport: FlakyTestReport): String = {
    if (flakyTestReport.flakyTests.exists(_.failures > 0)) {
      renderFailed(flakyTestReport)
    } else {
      renderNoFailures(flakyTestReport)
    }

  }

  def renderNoFailures(flakyTestReport: FlakyTestReport) = html(
    body(
      h1("Flaky test result"),
      p("All tests were succesfull")
    )
  ).render

  def renderFailed(flakyTestReport: FlakyTestReport) = {
    val timestamp = flakyTestReport.timeDetails.start
    val projectName = flakyTestReport.projectName
    val flaky = flakyTestReport.flakyTests
    val failedCount = flaky.count(_.failures > 0)
    val duration = flakyTestReport.timeDetails.duration()
    val timeSpend = TimeReport.formatSeconds(duration / 1000)
    val timeSpendPerIteration = TimeReport.formatSeconds((duration / flakyTestReport.testRuns.size) / 1000)


    val summaryTableContent: Seq[Text.TypedTag[String]] = flaky
      .filter(_.failures > 0)
      .sortBy(_.failurePercent())
      .reverse
      .map { flaky =>
        tr(
          td(a(href := s"#${flaky.test.clazz}", flaky.test.classNameOnly())),
          td(flaky.test.test),
          td(f"${flaky.failures * 100f / flaky.totalRun}%.2f%%")
        )
      }

    val summaryTable = table(border := "1px solid", backgroundColor := "yellow",
      thead(
        th(b("Class")),
        th("Test"),
        th("Failure rate")
      ),
      tbody(summaryTableContent)
    )
    val summaryAttachment =
      p(
        h1(s"Flaky test result for $projectName"),
        h2(s"Flaky test result: $failedCount test failed of ${flaky.size} tests. Test were running for $timeSpend [$timeSpendPerIteration/iteration]"),
        p(summaryTable)
      )


    val flakyCases: Map[String, List[FlakyCase]] = flakyTestReport.groupFlakyCases()
    val failedAttachments = flakyCases.map {
      case (testClass, flakyTestCases) =>
        val flakyTestsDescription = flakyTestCases
          .sortBy(_.runNames.size)
          .map {
            fc =>
              val test = fc.test
              val message = fc.message.getOrElse("?")
              val runNames = fc.runNames.sorted.mkString(", ")
              val text =
                p(
                  h4(s"${test.test} failed ${fc.runNames.size} times"),
                  p(s"Test failed in following runs $runNames"),
                  p(b(s"Stacktrace:"), code(fc.stacktrace)),
                  if (fc.allMessages.size == 1) {
                    p(b("Message: "), code(message))
                  } else {
                    p(b("Message common part:"),
                      code(message),
                      p("Detailed messages:"), ul(
                        fc.allMessages.map(m => li(code(m))).toArray: _*
                      )
                    )
                  }
                )
              text
          }
        p(hr(),
          h3(id := testClass, s"Details for ${testClass.split('.').lastOption.getOrElse("<?>")}"),
          flakyTestsDescription
        )

    }
    html(
      body(
        summaryAttachment,
        p(
          failedAttachments.toArray: _*
        )
      )
    ).render
  }

}
