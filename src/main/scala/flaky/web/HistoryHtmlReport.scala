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

  case class BuildDiff(historyReport: HistoryReportDescription, buildNr: Int, gitCommits: List[GitCommit])

  def calculateBuildDiffs(historyReportDescription: List[HistoryReportDescription], git: Git): List[BuildDiff] = {
    if (historyReportDescription.nonEmpty) {
      val descriptionsFromNewest: immutable.Seq[BuildDiff] = historyReportDescription
        .sortBy(_.timestamp)
        .zipWithIndex
        .reverse
        .map {
          case (hrd, index) => BuildDiff(hrd, index + 1, List.empty)
        }
      val tuples: immutable.Seq[(BuildDiff, Option[String])] = descriptionsFromNewest.zip(descriptionsFromNewest.tail.map(_.historyReport.gitCommitHash))
      val buildInfo: immutable.Seq[BuildDiff] = tuples.map {
        case (buildDiff, maybePreviousHash) =>
          val commits = for {
            currentCommit <- buildDiff.historyReport.gitCommitHash
            previousCommit <- maybePreviousHash
          } yield git.commitsList(previousCommit, currentCommit).getOrElse(List.empty)
          buildDiff.copy(gitCommits = commits.getOrElse(List.empty).sortBy(_.commitTime).reverse)
      }
      val firstBuild = descriptionsFromNewest.lastOption.toList
      buildInfo.toList ::: firstBuild
    } else {
      List.empty[BuildDiff]
    }
  }

  private def processChanges(historyReport: HistoryReport, git: Git) = {

    val buildDiffs: immutable.Seq[BuildDiff] = calculateBuildDiffs(historyReport.historicalRuns.map(_.historyReportDescription), git)

    val diffsHtml: immutable.Seq[Text.TypedTag[String]] = buildDiffs.flatMap {
      buildDiff =>
        val commits = buildDiff.gitCommits
        val hrdCurrent = buildDiff.historyReport

        // Build/hashes  | id | author | shortMsg
        val build = s"${buildDiff.buildNr} - ${new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(hrdCurrent.timestamp)}"
        val listOfCommits: immutable.Seq[GitCommit] = commits.sortBy(_.commitTime).reverse

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
              commitToRow(GitCommit("-", "-", "-", 0), ReportCss.diffTableTdFirstCommit))
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

    val processFunction: (((String, Seq[HistoryStat])) => Text.Modifier) = {
      x =>
        val clazz = x._1
        val stats = x._2
        val htmlForTests = stats.map { stat =>
          p(p(
            id := s"${stat.test.clazz}_${stat.test.test}",
            ReportCss.testName,
            s"Test: ${stat.test.test}",
            a(href := s"$currentResultFile#${stat.test.clazz}_${stat.test.test}", " <Details of last test>")
          ),
            SvgChart.failureChart(stat.stats.map(_.failureRate()))
          )
        }

        val clazzName: String = stats.headOption.map(_.test.classNameOnly()).getOrElse(clazz)
        p(
          h3(ReportCss.testClass, clazzName, id := s"$clazz"),
          htmlForTests
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

    val successProbability = historyReport.historicalRuns.map(_.report.successProbabilityPercent())
    println(s"Successes probability: $successProbability")

    val statsWithFailure: immutable.Seq[HistoryStat] = stats.filter(_.stats.exists(_.failedCount > 0))
    val groupedByClass: Map[String, immutable.Seq[HistoryStat]] = statsWithFailure.groupBy(_.test.clazz)

    val page = html(
      head(link(rel := "stylesheet", href := "report.css")),
      body(
        h1(ReportCss.title, s"History trends of flaky tests for ${historyReport.project}"),
        h2(ReportCss.subtitle, "Build success probability"),
        p(SvgChart.successChart(successProbability)),
        p("Build success probability is change that none of flaky test will fail during build."),
        processChanges(historyReport, git),
        p(a(href := currentResultFile, h3("Last detailed report"))),
        h2(ReportCss.subtitle, "Details"),
        p(groupedByClass.map(processFunction).toArray: _*),
        footer()
      )
    )
    page.render
  }
}


