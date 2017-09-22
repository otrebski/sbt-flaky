package flaky.web

import java.text.SimpleDateFormat
import java.util.Date

import flaky.{FlakyCase, FlakyTestReport, TimeReport}

import scalatags.Text
import scalatags.Text.all._

object HtmlSinglePage {

  def pageSource(flakyTestReport: FlakyTestReport, historyFile: Option[String]): String = {
    if (flakyTestReport.flakyTests.exists(_.failures > 0)) {
      renderFailed(flakyTestReport, historyFile)
    } else {
      renderNoFailures(flakyTestReport, historyFile)
    }
  }

  def renderNoFailures(flakyTestReport: FlakyTestReport, historyFile: Option[String]): String =
    html(
      head(link(rel := "stylesheet", href := "report.css")),
      body(
        h1(ReportCss.title, "Flaky test result"),
        p(ReportCss.allSuccess, "All tests were successful"),
        p(historyFile.map(f => a(href := s"$f", "History trends")))
      )
    ).render

  def successBarChar(failurePercent: Float): Text.TypedTag[String] = {
    import scalatags.Text.svgAttrs.{fill, x, y, height => svgHeight, width => svgWidth}
    import scalatags.Text.svgTags._
    val red = failurePercent.toInt
    val green = 100 - red
    svg(ReportCss.successBar, svgWidth := "100", svgHeight := "20")(
      rect(svgWidth := s"$green", svgHeight := 20, fill := "rgb(0,195,0)"),
      rect(x := s"$green", svgWidth := s"$red", svgHeight := 20, fill := "rgb(255,30,30)"),
      text(x := "10", y := "15")(f"${100 - failurePercent}%.2f%%")
    )
  }

  def failureBarChar(failed: Int, runs: Int): Text.TypedTag[String] = {
    successBarChar(100f * failed / runs)
  }


  def renderFailed(flakyTestReport: FlakyTestReport, historyFile: Option[String]): String = {
    val timestamp = flakyTestReport.timeDetails.start
    val projectName = flakyTestReport.projectName
    val flaky = flakyTestReport.flakyTests
    val failedCount = flaky.count(_.failures > 0)
    val duration = flakyTestReport.timeDetails.duration()
    val testRunsCount = flakyTestReport.testRuns.size
    val timeSpend = TimeReport.formatSeconds(duration / 1000)
    val timeSpendPerIteration = TimeReport.formatSeconds((duration / testRunsCount) / 1000)

    val summaryTableContent: Seq[Text.TypedTag[String]] = flaky
      .filter(_.failures > 0)
      .sortBy(_.failurePercent())
      .reverse
      .map { flaky =>
        val testName = if (flaky.test.test == "(It is not a test)") {
          "(It is not a test) - There is no  test name in JUnit report"
        } else flaky.test.test
        tr(
          td(ReportCss.summaryTableTd, a(href := s"#${anchorClass(flaky.test)}", flaky.test.classNameOnly())),
          td(ReportCss.summaryTableTd, a(href := s"#${anchorTest(flaky.test)}", testName)),
          td(ReportCss.summaryTableTd, successBarChar(flaky.failurePercent())),
          td(ReportCss.summaryTableTd, historyFile.map(f => a(href := s"$f#${anchorTest(flaky.test)}", img(src := "history.png"))))
        )
      }

    val summaryTable = table(ReportCss.summaryTable,
      thead(
        th(b("Class")),
        th("Test"),
        th("Success rate"),
        th("Trend")
      ),
      tbody(summaryTableContent)
    )
    val summaryAttachment =
      p(
        h1(ReportCss.title, s"Flaky test result for $projectName at ${new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(timestamp))}"),
        h2(ReportCss.subtitle, "Summary"),
        p(s"Flaky test result: $failedCount test failed of ${flaky.size} tests. Test were running for $timeSpend [$timeSpendPerIteration/iteration]"),
        p(ReportCss.successProbability, s"Build success probability: ", successBarChar(flakyTestReport.successProbabilityPercent())),
        p(summaryTable),
        p(i("Results are sorted from lowest success rate. Tests without failures are not displayed"))
      )

    val flakyCases: Map[String, List[FlakyCase]] = flakyTestReport.groupFlakyCases()

    def message(fc: FlakyCase) = {
      val message = fc.message

      if (fc.allMessages.size == 1) {
        p(ReportCss.message, b("Message: "), code(message))
      } else {
        p(
          p(ReportCss.message,
            b("Message common part:"),
            code(message)
          ),
          p("Detailed messages:"), ul(
            fc.allMessages.map(m => li(ReportCss.message, code(m))).toArray: _*
          )
        )
      }
    }

    val failedAttachments = flakyCases.map {
      case (testClass, flakyTestCases) =>
        val flakyTestsDescription = flakyTestCases
          .sortBy(_.runNames.size)
          .map {
            fc =>
              val test = fc.test
              val runNames = fc.runNames
                .sortWith((t1, t2) => t1.toInt < t2.toInt)
                .map(runName => a(s"$runName, ", href := linkToRunNameInSingleTest(test, runName)))

              val text =
                p(
                  h4(
                    id := s"${test.clazz}_${test.test}",
                    ReportCss.testName,
                    failureBarChar(fc.runNames.size, testRunsCount),
                    historyFile.map(f => a(href := s"$f#${anchorTest(test)}", img(ReportCss.historyIcon, src := "history.png", alt := "History trends"))),
                    s" ${test.test} failed ${fc.runNames.size} time${if (fc.runNames.size > 1) "s" else ""}"
                  ),
                  p(b(s"Stacktrace:"), code(fc.stacktrace)),
                  message(fc),
                  p(a(href := linkToSingleTest(test), s"Test failed in following runs: "), runNames)
                )
              text
          }
        p(hr(),
          h3(
            id := testClass,
            ReportCss.testClass,
            s"${testClass.split('.').lastOption.getOrElse(testClass)}"
          ),
          flakyTestsDescription
        )
    }
    html(
      head(link(rel := "stylesheet", href := "report.css")),
      body(
        ReportCss.body,
        summaryAttachment,
        h2(ReportCss.subtitle, "Details"),
        p(
          failedAttachments.toArray: _*
        ),
        footer()
      )
    ).render
  }


}
