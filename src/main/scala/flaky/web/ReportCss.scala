package flaky.web

import scalatags.Text.all._
import scalatags.Text.styles._
import scalatags.stylesheet._

object ReportCss extends StyleSheet {
  initStyleSheet()

  val allSuccess: Cls = cls(
    backgroundColor := "#33AA33"
  )

  val testClass: Cls = cls(
    backgroundColor := "LightPink"
  )

  val testName: Cls = cls(
    backgroundColor := "MistyRose"
  )

  val message: Cls = cls(
    border := "1px solid lightgray"
  )

  val summaryTable: Cls = cls(
    border := "1px solid",
    backgroundColor := "#CCCCCC"
  )

  val summaryTableTd: Cls = cls(
    borderTop := "1px solid"
  )

}