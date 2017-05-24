package flaky.web

import scala.collection.immutable.Range
import scalatags.text.Builder
import scalatags.{Text, generic}

object SvgChart {
  val graphWidth = 800
  val graphHeight: Int = 300
  private val textArea = 50

  import scalatags.Text.svgAttrs.{fill, points, stroke, strokeWidth, x, x1, x2, y, y1, y2}
  import scalatags.Text.svgTags._
  import scalatags.Text.all._

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
    import scalatags.Text.svgAttrs.{height => svgHeight, width => svgWidth}
    svg(svgWidth := (graphWidth + textArea).toString, svgHeight := (graphHeight + textArea).toString)(
      (List(series(failuresRate)) ::: axis()).toArray: _*
    )
  }

}
