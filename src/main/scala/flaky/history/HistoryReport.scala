package flaky.history

import flaky._

case class HistoricalRun(historyReportDescription: HistoryReportDescription, report: FlakyTestReport)

case class Stat(date: String, failedCount: Int, totalRun: Int) {
  def failureRate(): Float = {
    if (totalRun == 0) {
      0f
    } else {
      (failedCount * 100f) / totalRun
    }
  }
}

case class TestSummary(test: Test, stat: Stat)

case class HistoryStat(test: Test, stats: List[Stat] = List.empty[Stat])

case class HistoryReport(project: String, date: String, historicalRuns: List[HistoricalRun]) {

  def testSummary(historicalRuns: List[HistoricalRun]): List[TestSummary] = {
    val tests =
      historicalRuns
        .flatMap(_.report.testRuns)
        .flatMap(_.testCases)
        .map(_.test)
        .distinct

    for {
      test <- tests
      historicalRun <- historicalRuns
    } yield TestSummary(test, Stat(
      historicalRun.historyReportDescription.timestamp.toString,
      historicalRun.report.flakyTests.find(_.test == test).map(_.failedRuns.size).getOrElse(0),
      historicalRun.report.flakyTests.find(_.test == test).map(_.totalRun).getOrElse(0))
    )
  }

  def historyStat(): List[HistoryStat] = {
    testSummary(historicalRuns)
      .groupBy(_.test)
      .map {
        case (a, b) => HistoryStat(a, b.sortBy(_.stat.date).map(_.stat))
      }.toList.sortBy(_.test.test)
  }

}
