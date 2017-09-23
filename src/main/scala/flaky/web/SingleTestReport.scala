package flaky.web

import flaky.FlakyTest

import scalatags.Text.all._

object SingleTestReport {

  def pageSource(flakyTest: FlakyTest): String = {
    val ft = flakyTest.failedRuns
      .sortWith((t1, t2) => t1.runName.toInt < t2.runName.toInt)
      .map { testCase =>
        p(
          p(ReportCss.runName, id := anchorTestRun(testCase), s"Test run ${testCase.runName}"),
          testCase.failureDetails.map { fd =>
            p(
              p(ReportCss.exception, s"Exception: ${fd.ftype}"),
              p(ReportCss.message, s"Message: ${fd.message}"),
              pre(fd.stacktrace)
            )
          }.getOrElse(p(""))
        )
      }

    val indexes = flakyTest.failedRuns
      .sortWith((t1, t2) => t1.runName.toInt < t2.runName.toInt)
      .map { testCase =>
        div(a(href := s"#${anchorTestRun(testCase)}", s"${testCase.runName}: ${testCase.failureDetails.map(_.message).getOrElse("")}"))
      }

    html(head(link(rel := "stylesheet", href := "../report.css")),
      body(
        div(ReportCss.header,
          h1(ReportCss.title, s"Flaky test result for ${flakyTest.test.clazz}"),
          p(ReportCss.testClass, flakyTest.test.clazz),
          p(ReportCss.testName, flakyTest.test.test),
          p(indexes),
          p(s"Failed ${flakyTest.failures()} times"),
        ),
        div(ReportCss.content, p(ft: _*))
      )
    ).render
  }
}
