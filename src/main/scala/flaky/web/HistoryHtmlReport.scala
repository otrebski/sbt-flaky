package flaky.web

import java.io.File
import java.text.SimpleDateFormat

import flaky.Io
import flaky.history._

import scala.collection.immutable
import scalatags.Text
import scalatags.Text.all._
import scalatags.stylesheet.Cls


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
  Io.writeToFile(new File("historyChart.html"), renderHistory(historyReport1, Git(new File(".")), ""))

  private def processChanges(historyReport: HistoryReport, git: Git) = {
    val tuples = historyReport
      .historicalRuns
      .map(_.historyReportDescription)
      .zipWithIndex

    val diffs = tuples.zip(tuples.tail)
    val diffsHtml: immutable.Seq[Text.TypedTag[String]] = diffs.flatMap {
      case ((hrdPrev, _), (hrdCurrent, index)) =>
        val commits: Option[immutable.Seq[GitCommit]] = for {
          commitPrev <- hrdPrev.gitCommitHash
          commitNext <- hrdCurrent.gitCommitHash
        } yield git.commitsList(commitPrev, commitNext).getOrElse(List.empty)

        // Build/hashes  | id | author | shortMsg
        val build = s"$index - ${new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(hrdCurrent.timestamp)}"

        val listOfCommits: immutable.Seq[GitCommit] = commits.toList.flatten.reverse

        def commitToRow(first: GitCommit, style: Cls) = Seq(
          td(style, first.id),
          td(style, first.author),
          td(style, first.shortMsg)
        )

        val currentVersion: String = hrdCurrent.gitCommitHash.getOrElse("?")
        val changes: Seq[Text.TypedTag[String]] = listOfCommits match {
          case Nil => Seq(
            tr(
              td(ReportCss.diffTableTdBuild, build),
              td(ReportCss.diffTableTdGitHash, currentVersion),
              commitToRow(GitCommit("-", "-", "-"), ReportCss.diffTableTdFirstCommit))
          )
          case first :: tail =>
            tr(ReportCss.diffTableTr,
              td(rowspan := s"${tail.size + 1}", build, ReportCss.diffTableTdBuild),
              td(rowspan := s"${tail.size + 1}", currentVersion, ReportCss.diffTableTdGitHash),
              commitToRow(first, ReportCss.diffTableTdFirstCommit)) ::
              tail.map { c => tr(ReportCss.diffTableTr, commitToRow(c, ReportCss.diffTableTdCommit)) }
        }
        changes
    }
    val diffsTable = table(ReportCss.diffTable,
      tr(
        th(ReportCss.diffTableTh, "Build"),
        th(ReportCss.diffTableTh, "Git version"),
        th(ReportCss.diffTableTh, "Id"),
        th(ReportCss.diffTableTh, "Author"),
        th(ReportCss.diffTableTh, "Commit message")
      ),
      diffsHtml
    )
    p(h2(ReportCss.subtitle, "Changes"), diffsTable)
  }

  override def renderHistory(historyReport: HistoryReport, git: Git, currentResultFile: String): String = {

    val processFunction: (HistoryStat => Text.Modifier) = {
      t =>
        val failuresRate: Seq[Float] = t.stats.map(_.failureRate())
        p(
          h3(ReportCss.testClass, s"Class: ${t.test.classNameOnly()}", id := s"${t.test.clazz}_${t.test.test}"),
          p(
            ReportCss.testName,
            s"Test: ${t.test.test}",
            a(href := s"$currentResultFile#${t.test.clazz}_${t.test.test}", " <Details of last test>")
          ),
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
        h1(ReportCss.title, s"History trends of flaky tests for ${historyReport.project}"),
        h2(ReportCss.subtitle, "Average failure rate"),
        p(SvgChart.chart(summaryFailures)),
        processChanges(historyReport, git),
        p(a(href := currentResultFile, h3("Last detailed report"))),
        h2(ReportCss.subtitle, "Failures per class"),
        p(stats.filter(_.stats.exists(_.failedCount > 0)).map(processFunction).toArray: _*),
        footer()
      )
    )
    page.render
  }
}


