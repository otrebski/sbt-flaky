package flaky.history

import java.io.File

import flaky.Io
import flaky.report.HtmlSinglePage

import scala.collection.immutable.Range
import scalatags.Text
import scalatags.Text.all._


object SvgChart {
  val graphWidth = 800
  val graphHeight: Int = 300
  private val textArea = 50

  import scalatags.Text.svgAttrs._
  import scalatags.Text.svgTags._

  object Styles {
    val axis = style := "stroke:rgb(0,0,0);stroke-width:1"
    val horizontalLine = style := "stroke:rgb(200,200,200);stroke-width:1"
    val polygonStyle = style := "fill:#7AB295;stroke:black;stroke-width:1"
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
        Styles.axis
      )
    }
    val majorRicksY = Range(5, 28, 5).flatMap { value =>
      line(
        x1 := xCoordinate(0) - 4,
        x2 := graphWidth,
        y1 := yCoordinate(value),
        y2 := yCoordinate(value),
        Styles.horizontalLine
      ) ::
        line(
          x1 := xCoordinate(0) - 4,
          x2 := xCoordinate(0) + 4,
          y1 := yCoordinate(value),
          y2 := yCoordinate(value),
          Styles.axis
        ) :: text(x := xCoordinate(0) - 35, y := yCoordinate(value) + 7)(s"$value%") :: Nil
    }

    val ticksX = Range(0, 30).map { index =>
      line(
        x1 := xCoordinate(index),
        x2 := xCoordinate(index),
        y1 := (yCoordinate(0) - 3).toString,
        y2 := (yCoordinate(0) + 3).toString,
        Styles.axis
      )
    }

    val axis = List(line(x1 := textArea.toString, y1 := "0", x2 := textArea.toString, y2 := "300", Styles.axis),
      line(x1 := textArea.toString, y1 := "0", x2 := (textArea - 5).toString, y2 := "10", Styles.axis),
      line(x1 := textArea.toString, y1 := "0", x2 := (textArea + 5).toString, y2 := "10", Styles.axis),
      line(x1 := textArea.toString, y1 := graphHeight.toString, x2 := (graphWidth + textArea).toString, y2 := graphHeight.toString, Styles.axis),
      line(x1 := (graphWidth + textArea - 10).toString, y1 := (graphHeight - 5).toString, x2 := (graphWidth + textArea).toString, y2 := graphHeight.toString, Styles.axis),
      line(x1 := (graphWidth + textArea - 10).toString, y1 := (graphHeight + 5).toString, x2 := (graphWidth + textArea).toString, y2 := graphHeight.toString, Styles.axis))
    (axis ::: ticksX.toList ::: ticksY.toList ::: majorRicksY.toList).toList
  }


  def series(failures: List[Float]): Text.TypedTag[String] = {
    val pointsString = failures
      .zipWithIndex
      .map {
        case (value, index) => s"${xCoordinate(index)} ${yCoordinate(value)}"
      }.mkString(s"${xCoordinate(0)} ${yCoordinate(0)}, ", ",", s", ${xCoordinate(failures.size - 1)} ${yCoordinate(0)}")

    polygon(points := pointsString, Styles.polygonStyle)
  }


  def chart(failuresRate: List[Float]) = {
    svg(width := (graphWidth + textArea).toString, height := (graphHeight + textArea).toString)(
      (axis() ::: List(series(failuresRate))).toArray: _*
    )
  }



}

object HistoryHtmlReport extends App with HistoryReportRenderer {
  if (args.headOption.isEmpty) {
    println("Pass path to dir with history reports (zip files)")
    sys.exit(1)
  }
  private val dir = args.head
  private val history = new History("Project x", new File(dir), new File(""))
  println("Loading history data")
  private val historyReport1 = history.createHistoryReport()
  println("History data loaded")
  Io.writeToFile(new File("historyChart.html"), renderHistory(historyReport1))

  override def renderHistory(historyReport: HistoryReport): String = {
    val grouped = historyReport.grouped()

    val processFunction: (HistoryStat => Text.Modifier) = {
      t =>
        val failuresRate: Seq[Float] = t.stats.map(_.failureRate)
        p(
          h3(css("background-color") := HtmlSinglePage.testClassColor, s"Class: ${t.test.classNameOnly()}"),
          p(css("background-color") := HtmlSinglePage.testNameColor, s"Test: ${t.test.test}"),
          SvgChart.chart(failuresRate.toList)
        )
    }

    val page = html(
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
        HtmlSinglePage.footer()
      )
    )
    page.render
  }
}


