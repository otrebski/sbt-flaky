package flaky.web

import scala.collection.immutable.Range
import scalatags.text.Builder
import scalatags.{Text, generic}

object SvgChart {
  val graphWidth = 800
  val graphHeight: Int = 300
  private val textArea = 50

  import scalatags.Text.all._
  import scalatags.Text.svgAttrs.{fill, points, stroke, strokeWidth, textAnchor, x, x1, x2, y, y1, y2}
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
        Styles.horizontalLineStroke, Styles.horizontalLineStrokeWidth
      ) ::
        line(
          x1 := xCoordinate(0) - 4,
          x2 := xCoordinate(0) + 4,
          y1 := yCoordinate(value),
          y2 := yCoordinate(value),
          Styles.axisStroke, Styles.axisStrokeWidth
        ) :: text(x := xCoordinate(0) - 5, y := yCoordinate(value) + 7, textAnchor := "end")(s"$value%") :: Nil
    }

    val ticksX = Range(0, 30).toList.flatMap { index =>
      val xc = xCoordinate(index)
      List(
        line(
          x1 := xCoordinate(index),
          x2 := xCoordinate(index),
          y1 := (yCoordinate(0) - 3),
          y2 := (yCoordinate(0) + 3),
          Styles.axisStroke, Styles.axisStrokeWidth
        ),
        text(x := xc, y := yCoordinate(0) + 15, s"${index+1}", textAnchor := "middle")
      )
    }

    val axis = List(
      line(x1 := textArea, y1 := "0", x2 := textArea, y2 := "300", Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := textArea, y1 := "0", x2 := (textArea - 5), y2 := "10", Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := textArea, y1 := "0", x2 := (textArea + 5), y2 := "10", Styles.axisStroke, Styles.axisStrokeWidth),
      text(x := textArea + 10, y := 15, s"Failure ratio [%]", textAnchor := "start"),
      line(x1 := textArea, y1 := graphHeight, x2 := (graphWidth + textArea), y2 := graphHeight, Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := (graphWidth + textArea - 10), y1 := (graphHeight - 5), x2 := (graphWidth + textArea), y2 := graphHeight, Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := (graphWidth + textArea - 10), y1 := (graphHeight + 5), x2 := (graphWidth + textArea), y2 := graphHeight, Styles.axisStroke, Styles.axisStrokeWidth),
      text(x := (graphWidth + textArea - 10), y := yCoordinate(0) + 35, s"Build nr", textAnchor := "end")

    )
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
    svg(svgWidth := (graphWidth + textArea), svgHeight := (graphHeight + textArea))(
      (List(series(failuresRate)) ::: axis()).toArray: _*
    )
  }

}
