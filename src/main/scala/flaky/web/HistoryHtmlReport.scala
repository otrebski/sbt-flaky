package flaky.web

import java.io.File
import java.text.SimpleDateFormat

import flaky.Io
import flaky.history._

import scala.collection.immutable
import scalatags.Text
import scalatags.Text.all._


object HistoryHtmlReport extends App with HistoryReportRenderer {
  if (args.headOption.isEmpty) {
    println("Pass path to dir with history reports (zip files)")
    sys.exit(1)
  }
  private val dir = args.head
  private val history = new History("Project x", new File(dir), new File(""), new File("."))
  println("Loading history data")
  private val historyReport1 = history.createHistoryReport()
  println("History data loaded")
  Io.writeToFile(new File("historyChart.html"), renderHistory(historyReport1, Git(new File("."))))

  private def processChanges(historyReport: HistoryReport, git: Git) = {
    val tuples = historyReport
      .historicalRuns
      .map(_.historyReportDescription)
      .zipWithIndex

    val diffs = tuples.zip(tuples.tail)
    val diffsHtml: Seq[Text.TypedTag[String]] = diffs.map {
      case ((hrdPrev, _), (hrdNext, index)) =>
        val commits: Option[immutable.Seq[GitCommit]] = for {
          commitPrev <- hrdPrev.gitCommitHash
          commitNext <- hrdNext.gitCommitHash
        } yield git.commitsList(commitPrev, commitNext).getOrElse(List.empty)

        val changes = if (commits.toList.flatten.isEmpty) {
          p("No changes")
        } else {
          val commitsList: Array[Text.TypedTag[String]] = commits
            .map(c => c.map(commit => li(s"${commit.id}: ${commit.author} => ${commit.shortMsg}")))
            .map(ol(_))
            .toArray
          p(
            s"Changes ${hrdPrev.gitCommitHash.getOrElse("?")} -> ${hrdNext.gitCommitHash.getOrElse("?")}",
            commitsList
          )
        }
        p(
          h4(s"Build $index ${new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(hrdNext.timestamp)}"),
          changes
        )
    }
    p(h3("Changes:"), diffsHtml)
  }

  override def renderHistory(historyReport: HistoryReport, git: Git): String = {

    val processFunction: (HistoryStat => Text.Modifier) = {
      t =>
        val failuresRate: Seq[Float] = t.stats.map(_.failureRate())
        p(
          h3(ReportCss.testClass, s"Class: ${t.test.classNameOnly()}"),
          p(ReportCss.testName, s"Test: ${t.test.test}"),
          SvgChart.chart(failuresRate.toList)
        )
    }

    val stats: List[HistoryStat] = historyReport.historyStat()

    val summaryFailures: immutable.List[Float] = stats
      .flatMap(_.stats)
      .groupBy(_.date)
      .map {
        case (date, s1) =>
          s1.foldLeft(Stat(date, 0, 0))((acc, stat) =>
            Stat(date, acc.failedCount + stat.failedCount, acc.totalRun + stat.totalRun)
          )
      }
      .toList
      .sortBy(_.date)
      .map(_.failureRate())

    val page = html(
      head(link(rel := "stylesheet", href := "report.css")),
      body(
        h1(s"History trends of flaky tests for ${historyReport.project}"),
        p(s"Generate at ${historyReport.date}"),
        h3("Average failure rate"),
        p(SvgChart.chart(summaryFailures)),
        h3("Failures per class"),
        p(stats.filter(_.stats.exists(_.failedCount > 0)).map(processFunction).toArray: _*),
        hr(),
        processChanges(historyReport, git),
        footer()
      )
    )
    page.render
  }
}


