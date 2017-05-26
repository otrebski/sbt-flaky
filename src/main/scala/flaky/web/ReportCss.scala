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
    border := "1px solid",
    borderLeft := "5px solid",
    borderCollapse := "collapse",
    borderRadius := "5px"

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
    borderLeftWidth := "10px",
    color := "darkmagenta",
    textAlign := "center"
  )

  val subtitle: Cls = cls(
    border := "solid black 1px",
    borderLeftWidth := "5px",
    borderRadius := "5px",
    paddingLeft := "20px",
    color := "#014a08",
    backgroundColor := "antiquewhite"
  )

  val diffTable: Cls = cls(
    border := "1px solid",
    borderLeft := "5px solid",
    borderCollapse := "collapse",
    borderRadius := "5px"
  )

  val diffTableTr: Cls = cls(
    verticalAlign := "text-top",
    padding := "3px"
  )

  val diffTableTdCommit: Cls = cls(
    verticalAlign := "text-top",
    paddingLeft := "10px",
    fontFamily := "monospace"
  )
  val diffTableTdFirstCommit: Cls = cls(
    borderTop := "1px solid black",
    verticalAlign := "text-top",
    paddingLeft := "10px",
    fontFamily := "monospace"
  )

  val diffTableTdBuild: Cls = cls(
    borderTop := "1px solid black",
    verticalAlign := "text-top",
    paddingTop := "5px",
    paddingLeft := "10px",
    paddingRight := "10px"
  )

  val diffTableTdGitHash: Cls = cls(
    borderTop := "1px solid black",
    borderRight := "1px dashed lightgray",
    verticalAlign := "text-top",
    paddingTop := "5px",
    paddingLeft := "10px",
    paddingRight := "10px",
    fontFamily := "monospace"
  )

  val diffTableTh: Cls = cls(
    borderTop := "1px solid black",
    backgroundColor := "lightblue",
    color := "darkblue",
    textAlign := "center",
    paddingLeft := "10px",
    paddingRight := "10px"

  )

  val body: Cls = cls(
    backgroundColor := "#fafafa"
  )

  val footer: Cls = cls (
    color := "gray",
    textAlign:="right"
  )
}