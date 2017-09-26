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

  case class BuildDiff(
                        historyReport: HistoryReportDescription,
                        buildNr: Int,
                        gitCommits: List[GitCommit] = List.empty,
                        commitForPreviousBuild: Option[String] = None)

  def calculateBuildDiffs(historyReportDescription: List[HistoryReportDescription], git: Git): List[BuildDiff] = {
    if (historyReportDescription.nonEmpty) {
      val descriptionsFromNewest: immutable.Seq[BuildDiff] = historyReportDescription
        .sortBy(_.timestamp)
        .zipWithIndex
        .reverse
        .map {
          case (hrd, index) => BuildDiff(hrd, index + 1)
        }
      val tuples: immutable.Seq[(BuildDiff, Option[String])] = descriptionsFromNewest.zip(descriptionsFromNewest.tail.map(_.historyReport.gitCommitHash))
      val buildInfo: immutable.Seq[BuildDiff] = tuples.map {
        case (buildDiff, maybePreviousHash) =>
          val commits = for {
            currentCommit <- buildDiff.historyReport.gitCommitHash
            previousCommit <- maybePreviousHash
          } yield git.commitsList(previousCommit, currentCommit).getOrElse(List.empty)
          buildDiff.copy(
            gitCommits = commits.getOrElse(List.empty).sortBy(_.commitTime).reverse,
            commitForPreviousBuild = maybePreviousHash
          )
      }
      val firstBuild = descriptionsFromNewest.lastOption.toList
      buildInfo.toList ::: firstBuild
    } else {
      List.empty[BuildDiff]
    }
  }

  private def processChanges(historyReport: HistoryReport, git: Git) = {
    val gitRepo: Option[GitRepo] = git.remoteUrl().toOption.flatMap(GitRepo.fromUrl)
    val buildDiffs: immutable.Seq[BuildDiff] = calculateBuildDiffs(historyReport.historicalRuns.map(_.historyReportDescription), git)

    val diffsHtml: immutable.Seq[Text.TypedTag[String]] = buildDiffs.flatMap {
      buildDiff =>
        val commits = buildDiff.gitCommits
        val hrdCurrent = buildDiff.historyReport

        // Build/hashes  | id | author | shortMsg
        val build = s"${buildDiff.buildNr} - ${new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(hrdCurrent.timestamp)}"
        val listOfCommits: immutable.Seq[GitCommit] = commits.sortBy(_.commitTime).reverse

        def commitToRow(commit: Option[GitCommit], style: Cls) = {

          val idCell: Option[Text.TypedTag[String]] = for {
            c <- commit
            gr <- gitRepo
          } yield td(
            style,
            a(
              href := gr.commitLink(c),
              title := "Click to open commit details",
              c.id,
              img(ReportCss.gitIcon, src := "git.png")
            )
          )
          val id: Text.TypedTag[String] = idCell.getOrElse(td(style, "-"))
          val author = commit.map(_.author).getOrElse("-")
          val message = commit.map(_.shortMsg).getOrElse("-")
          Seq(
            id,
            td(style, author),
            td(style, message)
          )
        }

        val link: Option[String] = for {
          current <- hrdCurrent.gitCommitHash
          previous <- buildDiff.commitForPreviousBuild if current != previous
          gr <- gitRepo
        } yield gr.diffLink(previous, current)
        val currentGitHash = {
          hrdCurrent.gitCommitHash.getOrElse("?")
        }
        val currentVersion: Text.TypedTag[String] =
          link
            .map(l => a(href := l, currentGitHash, title := "Click to open diff between builds", img(ReportCss.diffIcon, src := "diff.png")))
            .getOrElse(p(currentGitHash))

        val changes: Seq[Text.TypedTag[String]] = listOfCommits match {
          case Nil => Seq(
            tr(
              td(ReportCss.diffTableTdBuild, build),
              td(ReportCss.diffTableTdGitHash, currentVersion),
              commitToRow(None, ReportCss.diffTableTdFirstCommit))
          )
          case first :: tail =>
            tr(ReportCss.diffTableTr,
              td(rowspan := s"${tail.size + 1}", build, ReportCss.diffTableTdBuild),
              td(rowspan := s"${tail.size + 1}", currentVersion, ReportCss.diffTableTdGitHash),
              commitToRow(Some(first), ReportCss.diffTableTdFirstCommit)) ::
              tail.map { c => tr(ReportCss.diffTableTr, commitToRow(Some(c), ReportCss.diffTableTdCommit)) }
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
            a(href := s"$currentResultFile#${anchorTest(stat.test)}", " <Details of last test>")
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

    val successProbability = historyReport.historicalRuns.map(_.report.successProbabilityPercent())

    val statsWithFailure: immutable.Seq[HistoryStat] = stats.filter(_.stats.exists(_.failedCount > 0))
    val groupedByClass: Map[String, immutable.Seq[HistoryStat]] = statsWithFailure.groupBy(_.test.clazz)

    val remote: String = git.remoteUrl().toOption.map(_.toString).getOrElse("Git repo unkonw")
    val gitRepo: String = git.remoteUrl().toOption.flatMap(GitRepo.fromUrl).map(_.toString).getOrElse("Git repo unkonw")
    val page = html(
      head(link(rel := "stylesheet", href := "report.css")),
      body(
        h1(ReportCss.title, s"History trends of flaky tests for ${historyReport.project}"),
        h2(ReportCss.subtitle, "Build success probability"),
        p(SvgChart.successChart(successProbability)),
        p("Build success probability is chance that none of tests will fail during build."),
        code(
          "P = 100 * sr1 * sr2 * ... * srN",
          br,
          "sr1 = passed count / total test runs count",
          br,
          "sr1: success rate for a single test case"
        ),
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


