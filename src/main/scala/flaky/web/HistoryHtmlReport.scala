package flaky.web

import java.io.File
import java.text.SimpleDateFormat

import flaky.Io
import flaky.history._

import scala.collection.immutable
import scala.collection.immutable.Range
import scalatags.Text.all._
import scalatags.text.Builder
import scalatags.{Text, generic}

object SvgChart {
  val graphWidth = 800
  val graphHeight: Int = 300
  private val textArea = 50

  import scalatags.Text.svgAttrs.{fill, stroke, strokeWidth, x, x1, x2, y, y1, y2, points}
  import scalatags.Text.svgTags._

  object Styles {
    val axisStroke: generic.AttrPair[Builder, String] = stroke := "black"
    val axisStrokeWidth: generic.AttrPair[Builder, String] = strokeWidth := "1"

    val horizontalLineStroke: generic.AttrPair[Builder, String] = stroke := "rgb(200,200,200)"
    val horizontalLineStrokeWidth: generic.AttrPair[Builder, String] = strokeWidth := "1"

    val polygonFill: generic.AttrPair[Builder, String] = fill := "red"
    val polygonStroke: generic.AttrPair[Builder, String] = stroke := "black"
    val polygonStrokeWidth: generic.AttrPair[Builder, String] = strokeWidth := "1"

  }

  def xCoordinate(index: Int): Int = textArea + 25 * index

  def yCoordinate(percent: Float): Int = (graphHeight - 10 * percent).toInt

  def axis(): List[Text.TypedTag[String]] = {
    val ticksY = Range(0, 28, 1).map { value =>
      line(
        x1 := xCoordinate(0) - 2,
        x2 := xCoordinate(0) + 2,
        y1 := yCoordinate(value),
        y2 := yCoordinate(value),
        Styles.axisStroke, Styles.axisStrokeWidth
      )
    }
    val majorRicksY = Range(5, 28, 5).flatMap { value =>
      line(
        x1 := xCoordinate(0) - 4,
        x2 := graphWidth,
        y1 := yCoordinate(value),
        y2 := yCoordinate(value),
        Styles.horizontalLineStroke,Styles.horizontalLineStrokeWidth
      ) ::
        line(
          x1 := xCoordinate(0) - 4,
          x2 := xCoordinate(0) + 4,
          y1 := yCoordinate(value),
          y2 := yCoordinate(value),
          Styles.axisStroke, Styles.axisStrokeWidth
        ) :: text(x := xCoordinate(0) - 35, y := yCoordinate(value) + 7)(s"$value%") :: Nil
    }

    val ticksX = Range(0, 30).map { index =>
      line(
        x1 := xCoordinate(index),
        x2 := xCoordinate(index),
        y1 := (yCoordinate(0) - 3).toString,
        y2 := (yCoordinate(0) + 3).toString,
        Styles.axisStroke, Styles.axisStrokeWidth
      )
    }

    val axis = List(line(x1 := textArea.toString, y1 := "0", x2 := textArea.toString, y2 := "300", Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := textArea.toString, y1 := "0", x2 := (textArea - 5).toString, y2 := "10", Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := textArea.toString, y1 := "0", x2 := (textArea + 5).toString, y2 := "10", Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := textArea.toString, y1 := graphHeight.toString, x2 := (graphWidth + textArea).toString, y2 := graphHeight.toString, Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := (graphWidth + textArea - 10).toString, y1 := (graphHeight - 5).toString, x2 := (graphWidth + textArea).toString, y2 := graphHeight.toString, Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := (graphWidth + textArea - 10).toString, y1 := (graphHeight + 5).toString, x2 := (graphWidth + textArea).toString, y2 := graphHeight.toString, Styles.axisStroke, Styles.axisStrokeWidth))
    axis ::: ticksX.toList ::: ticksY.toList ::: majorRicksY.toList
  }


  def series(failures: List[Float]): Text.TypedTag[String] = {
    val pointsString = failures
      .zipWithIndex
      .map {
        case (currentValue: Float, index) => s"${xCoordinate(index)} ${yCoordinate(currentValue)}"
      }.mkString(s"${xCoordinate(0)} ${yCoordinate(0)}, ", ",", s", ${xCoordinate(failures.size - 1)} ${yCoordinate(0)}")

    polygon(points := pointsString, Styles.polygonFill, Styles.polygonStroke, Styles.polygonStrokeWidth)
  }


  def chart(failuresRate: List[Float]): Text.TypedTag[String] = {
    svg(width := (graphWidth + textArea).toString, height := (graphHeight + textArea).toString)(
      (List(series(failuresRate)) ::: axis()).toArray: _*
    )
  }

}

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
    val grouped = historyReport.grouped()

    val processFunction: (HistoryStat => Text.Modifier) = {
      t =>
        val failuresRate: Seq[Float] = t.stats.map(_.failureRate)
        p(
          h3(ReportCss.testClass, s"Class: ${t.test.classNameOnly()}"),
          p(ReportCss.testName, s"Test: ${t.test.test}"),
          SvgChart.chart(failuresRate.toList)
        )
    }

    val page = html(
      head(link(rel := "stylesheet", href := "report.css")),
      body(
        h1(s"History trends of flaky tests for ${historyReport.project}"),
        p(s"Generate at ${historyReport.date}"),
        h2("New cases:"),
        p(grouped.newCases.map(processFunction).toArray: _*),
        hr(), hr(),
        h2("Worse:"),
        p(grouped.worse.map(processFunction).toArray: _*),
        hr(), hr(),
        h2("No change:"),
        p(grouped.noChange.map(processFunction).toArray: _*),
        hr(), hr(),
        h2("Improvment:"),
        p(grouped.better.map(processFunction).toArray: _*),
        hr(),
        processChanges(historyReport, git),
        footer()
      )
    )
    page.render
  }
}


