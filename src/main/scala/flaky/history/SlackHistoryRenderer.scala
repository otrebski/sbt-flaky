package flaky.history

import flaky.report.SlackReport.ToJsonString

class SlackHistoryRenderer extends HistoryReportRenderer {
  override def renderHistory(historyReport: HistoryReport): String = {
    val history = new TextHistoryReportRenderer().renderHistory(historyReport)
    val json = new ToJsonString(history).escapeJson()
    s"""
       |{
       |    "text": "$json"
       |}
     """.stripMargin

  }
}
