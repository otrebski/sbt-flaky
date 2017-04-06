package flaky.history

trait HistoryReportRenderer {
  def renderHistory(historyReport: HistoryReport): String
}
