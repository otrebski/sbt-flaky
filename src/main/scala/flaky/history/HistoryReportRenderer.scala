package flaky.history

trait HistoryReportRenderer {
  def renderHistory(historyReport: HistoryReport, git: Git, currentResultFile: String): String
}
