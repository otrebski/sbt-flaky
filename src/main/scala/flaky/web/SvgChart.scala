package flaky.web

import scala.collection.immutable.Range
import scalatags.text.Builder
import scalatags.{Text, generic}

object SvgChart {
  private val graphWidth = 800
  private val graphHeight: Int = 300
  private val textArea = 50

  import scalatags.Text.all._
  import scalatags.Text.svgAttrs.{fill, points, stroke, strokeWidth, textAnchor, x, x1, x2, y, y1, y2}
  import scalatags.Text.svgTags._

  object Styles {
    val axisStroke: generic.AttrPair[Builder, String] = stroke := "black"
    val axisStrokeWidth: generic.AttrPair[Builder, String] = strokeWidth := "1"

    val horizontalLineStroke: generic.AttrPair[Builder, String] = stroke := "rgb(200,200,200)"
    val horizontalLineStrokeWidth: generic.AttrPair[Builder, String] = strokeWidth := "1"

    val polygonFillRed: generic.AttrPair[Builder, String] = fill := "red"
    val polygonFillGreen: generic.AttrPair[Builder, String] = fill := "green"
    val polygonStroke: generic.AttrPair[Builder, String] = stroke := "black"
    val polygonStrokeWidth: generic.AttrPair[Builder, String] = strokeWidth := "1"

  }

  private def xCoordinate(index: Int): Int = textArea + 25 * index

  private def yCoordinate(viewPort:Range)(value:Float): Int = {
    //values => viewPort => coordinates on screen
    val zoom = (graphHeight/100)*100*(value-viewPort.start)/(viewPort.end-viewPort.start)
    val coordinate = (graphHeight-zoom).toInt
    coordinate
  }

  private def axis(yAxisTitle: String, yValueMapping: (Float => Int), valuesRange: Range): List[Text.TypedTag[String]] = {
    val rangeY = Range(valuesRange.start, valuesRange.end, 1)
    val ticksY = rangeY.map { value =>
      line(
        x1 := xCoordinate(0) - 2,
        x2 := xCoordinate(0) + 2,
        y1 := yValueMapping(value),
        y2 := yValueMapping(value),
        Styles.axisStroke, Styles.axisStrokeWidth
      )
    }

    val majorAxisTick = (valuesRange.end-valuesRange.start)/10
    val majorTicksY = Range(valuesRange.start, valuesRange.end, majorAxisTick).tail.flatMap { value =>
      line(
        x1 := xCoordinate(0) - 4,
        x2 := graphWidth,
        y1 := yValueMapping(value),
        y2 := yValueMapping(value),
        Styles.horizontalLineStroke, Styles.horizontalLineStrokeWidth
      ) ::
        line(
          x1 := xCoordinate(0) - 4,
          x2 := xCoordinate(0) + 4,
          y1 := yValueMapping(value),
          y2 := yValueMapping(value),
          Styles.axisStroke, Styles.axisStrokeWidth
        ) :: text(x := xCoordinate(0) - 5, y := yValueMapping(value) + 7, textAnchor := "end")(s"$value%") :: Nil
    }
    val ticksX = Range(0, 30).toList.flatMap { index =>
      val xc = xCoordinate(index)
      List(
        line(
          x1 := xCoordinate(index),
          x2 := xCoordinate(index),
          y1 := (graphHeight - 3),
          y2 := (graphHeight + 3),
          Styles.axisStroke, Styles.axisStrokeWidth
        ),
        text(x := xc, y := graphHeight + 15, s"${index + 1}", textAnchor := "middle")
      )
    }




    val axis = List(
      line(x1 := textArea, y1 := "0", x2 := textArea, y2 := "300", Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := textArea, y1 := "0", x2 := (textArea - 5), y2 := "10", Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := textArea, y1 := "0", x2 := (textArea + 5), y2 := "10", Styles.axisStroke, Styles.axisStrokeWidth),
      text(x := textArea + 10, y := 15, yAxisTitle, textAnchor := "start"),
      line(x1 := textArea, y1 := graphHeight, x2 := (graphWidth + textArea), y2 := graphHeight, Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := (graphWidth + textArea - 10), y1 := (graphHeight - 5), x2 := (graphWidth + textArea), y2 := graphHeight, Styles.axisStroke, Styles.axisStrokeWidth),
      line(x1 := (graphWidth + textArea - 10), y1 := (graphHeight + 5), x2 := (graphWidth + textArea), y2 := graphHeight, Styles.axisStroke, Styles.axisStrokeWidth),
      text(x := (graphWidth + textArea - 10), y := graphHeight + 35, s"Build nr", textAnchor := "end")

    )
    axis ::: ticksX ::: ticksY.toList ::: majorTicksY.toList
  }


  private def series(yValueMapping: (Float => Int), data: List[Float], polygonFill: generic.AttrPair[Builder, String] ): Text.TypedTag[String] = {
    val pointsString = data
      .zipWithIndex
      .map {
        case (currentValue: Float, index) => s"${xCoordinate(index)} ${yValueMapping(currentValue)}"
      }.mkString(s"${xCoordinate(0)} ${yValueMapping(0)}, ", ",", s", ${xCoordinate(data.size - 1)} ${yValueMapping(0)}")

    polygon(points := pointsString, polygonFill, Styles.polygonStroke, Styles.polygonStrokeWidth)
  }


  private def chart(
                     yAxisTitle: String,
                     data: List[Float],
                     yMapping: (Float => Int),
                     valuesRange: Range,
                     polygonFill: generic.AttrPair[Builder, String]
                   ) = {
    import scalatags.Text.svgAttrs.{height => svgHeight, width => svgWidth}
    svg(svgWidth := (graphWidth + textArea), svgHeight := (graphHeight + textArea))(
      (List(series(yMapping, data, polygonFill)) ::: axis(yAxisTitle, yMapping, valuesRange)).toArray: _*
    )
  }

  def successChart(successRate: List[Float]): Text.TypedTag[String] = {
    val range = Range(0, 109)
    chart("Build success probability [%]", successRate, yCoordinate(range), range, Styles.polygonFillGreen)
  }

  def failureChart(failuresRate: List[Float]): Text.TypedTag[String] = {
    val range = Range(0, 50)
    chart("Failure ratio [%]", failuresRate, yCoordinate(range), range, Styles.polygonFillRed)
  }

}
