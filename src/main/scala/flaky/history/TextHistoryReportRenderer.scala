package flaky.history

class TextHistoryReportRenderer extends HistoryReportRenderer {
  override def renderHistory(historyReport: HistoryReport, git:Git): String = {
    val grouped = historyReport.grouped()

    def summary(historyStat: List[HistoryStat]) = {
      historyStat
        .map { t =>
          val failures = t.stats.map(s => f"${s.failureRate()}%1.1f%%").mkString(", ")
          s"${t.test.clazz}.${t.test.test} Failure rate: $failures"
        }
        .toSet
        .mkString("\n", "\n", "\n")
    }

    s""" History trends:
       |New cases: ${summary(grouped.newCases)}
       |Improvement: ${summary(grouped.better)}
       |No change: ${summary(grouped.noChange)}
       |Worse: ${summary(grouped.worse)}
     """.stripMargin
  }
}
