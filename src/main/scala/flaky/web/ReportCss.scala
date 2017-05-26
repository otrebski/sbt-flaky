package flaky.web

import scalatags.Text.all._
import scalatags.stylesheet._

object ReportCss extends StyleSheet {
  initStyleSheet()

  val allSuccess: Cls = cls(
    backgroundColor := "#33AA33"
  )

  val testClass: Cls = cls(
    backgroundColor := "black",
    color := "#d4d4d4",
    padding := "5px",
    margin := "5px",
    border := "2px solid darkblue",
    borderRadius := "5px"
  )

  val testName: Cls = cls(
    backgroundColor := "lightblue",
    color := "darkblue",
    padding := "5px",
    margin := "5px",
    border := "1px solid darkblue",
    borderRadius := "5px"
  )

  val message: Cls = cls(
    border := "1px solid lightgray",
    padding := "5px"
  )

  val summaryTable: Cls = cls(
    border := "2px dotted",
    backgroundColor := "lavender"

  )

  val summaryTableTd: Cls = cls(
    borderTop := "1px solid lightblue",
    color := "black",
    paddingLeft := "4px"
  )

  val historyIcon: Cls = cls(
    paddingLeft := "3px"
  )

  val title: Cls = cls(
    border := "solid black 1px",
    borderRadius := "10px",
    borderLeftWidth:="10px",
    color := "darkmagenta",
    textAlign := "center"
  )

  val subtitle: Cls = cls(
    border := "solid black 1px",
    borderLeftWidth:="5px",
    borderRadius := "5px",
    paddingLeft := "20px",
    color := "#014a08",
    backgroundColor := "antiquewhite"
  )

  val body: Cls = cls(
    backgroundColor := "#fafafa"
  )
}