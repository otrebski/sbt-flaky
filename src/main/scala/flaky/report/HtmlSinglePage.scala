package flaky.report

import flaky.{FlakyCase, FlakyTestReport, TimeReport}

import scalatags.Text
import scalatags.Text.all._

object HtmlSinglePage {

  val testNameColor = "MistyRose"
  val testClassColor = "LightPink"


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

  def failureBarChar(failurePercent: Float): Text.TypedTag[String] = {
    import scalatags.Text.svgAttrs.{x, y}
    import scalatags.Text.svgTags._
    val red = failurePercent.toInt
    val green = 100 - red
    svg(width := "100", height := "20")(
      rect(width := s"$green", height := 20, style := "fill:rgb(0,195,0)"),
      rect(x := s"$green", width := s"$red", height := 20, style := "fill:rgb(255,0,0)"),
      text(x := "10", y := "15")(f"$failurePercent%.2f%%")
    )
  }

  def failureBarChar(failed: Int, runs: Int): Text.TypedTag[String] = {
    failureBarChar(100f * failed / runs)
  }


  def renderFailed(flakyTestReport: FlakyTestReport) = {
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
        tr(
          td(a(href := s"#${flaky.test.clazz}", flaky.test.classNameOnly())),
          td(flaky.test.test),
          td(failureBarChar(flaky.failurePercent()))
        )
      }

    val summaryTable = table(border := "1px solid", backgroundColor := "#CCCCCC",
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

    def message(fc: FlakyCase) = {
      val message = fc.message
      val lightBorder = css("border") := "1px solid lightgray"
      if (fc.allMessages.size == 1) {
        p(lightBorder, b("Message: "), code(message))
      } else {
        p(
          p(lightBorder,
            b("Message common part:"),
            code(message)
          ),
          p("Detailed messages:"), ul(
            fc.allMessages.map(m => li(lightBorder, code(m))).toArray: _*
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
              val runNames = fc.runNames.sorted.mkString(", ")
              val text =
                p(
                  h4(
                    css("background-color") := testNameColor,
                    failureBarChar(fc.runNames.size, testRunsCount),
                    s" ${test.test} failed ${fc.runNames.size} times"
                  ),
                  p(b(s"Stacktrace:"), code(fc.stacktrace)),
                  message(fc),
                  p(s"Test failed in following runs $runNames")
                )
              text
          }
        p(hr(),
          h3(
            id := testClass,
            css("background-color") := testClassColor,
            s"Details for ${testClass.split('.').lastOption.getOrElse("<?>")}"
          ),
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
