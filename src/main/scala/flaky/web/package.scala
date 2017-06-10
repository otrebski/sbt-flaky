package flaky

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

import scalatags.Text
import scalatags.Text.all.{a, hr, href, p, _}

package object web {
  def footer(): Text.TypedTag[String] = {
    p(
      hr(),
      p(
        ReportCss.footer,
        "Created with ",
        a(href := "https://github.com/otrebski/sbt-flaky", "sbt-flaky plugin"), br,
        s"Report generated at ${new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())}",
        s"Fugue icons are on Creative Common license"
      )
    )
  }

  def indexHtml(reportFile: File, historyFile: Option[File]): String = {
    val history = historyFile match {
      case Some(fileName) => a(href := fileName.getName, "History trends")
      case None =>
        p(
          "History trends report is not created. To enable history check documentation at ",
          a(href := "https://github.com/otrebski/sbt-flaky", "https://github.com/otrebski/sbt-flaky")
        )
    }

    html(
      head(link(rel := "stylesheet", href := "report.css")),
      body(
        h1(ReportCss.title, "Flaky test report"),
        h4(ReportCss.subtitle, a(href := reportFile.getName, "Report for last build")),
        h4(ReportCss.subtitle, history),
        footer()
      )
    ).render
  }
}
