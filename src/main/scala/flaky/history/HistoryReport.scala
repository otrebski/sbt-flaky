package flaky.history

import flaky._

case class HistoricalRun(historyReportDescription: HistoryReportDescription, report: FlakyTestReport)

case class Stat(date: String, failureRate: Float)

case class TestSummary(test: Test, stat: Stat)

case class HistoryStat(test: Test, stats: List[Stat] = List.empty[Stat])

case class Grouped(
                    good: List[HistoryStat] = List.empty[HistoryStat],
                    newCases: List[HistoryStat] = List.empty[HistoryStat],
                    fixed: List[HistoryStat] = List.empty[HistoryStat],
                    better: List[HistoryStat] = List.empty[HistoryStat],
                    noChange: List[HistoryStat] = List.empty[HistoryStat],
                    worse: List[HistoryStat] = List.empty[HistoryStat]) {

  def all(): List[HistoryStat] = {
    good ::: newCases ::: fixed ::: better ::: noChange ::: worse
  }

}

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
    } yield TestSummary(test, Stat(historicalRun.historyReportDescription.timestamp.toString, historicalRun.report.flakyTests.find(_.test == test).map(_.failurePercent()).getOrElse(0f)))
  }

  def historyStat(): List[HistoryStat] = {
    testSummary(historicalRuns)
      .groupBy(_.test)
      .map {
        case (a, b) => HistoryStat(a, b.sortBy(_.stat.date).map(_.stat))
      }.toList.sortBy(_.test.test)
  }

  def grouped(): Grouped = {
    HistoryReport.grouped(testSummary(historicalRuns))
  }
}

object HistoryReport {

  def grouped(testsSummary: List[TestSummary]): Grouped = {
    testsSummary
      .groupBy(_.test)
      .foldLeft(Grouped()) { (group, tl) =>
        val test = tl._1
        val stats = tl._2.sortBy(_.stat.date).map(_.stat)
        HistoryReport.groupTestResult(group, test, stats)
      }
  }

  def groupTestResult(group: Grouped, test: Test, stats: List[Stat]): Grouped = {
    if (stats.size > 1) {
      val last2 = stats.drop(stats.size - 2)
      val ratePrevious = last2.head.failureRate
      val last = last2(1).failureRate
      if (!stats.exists(_.failureRate > 0)) {
        group.copy(good = HistoryStat(test, stats) :: group.good)
      } else if (ratePrevious == 0 && last > 0) {
        group.copy(newCases = HistoryStat(test, stats) :: group.newCases)
      } else if (ratePrevious > last && last == 0) {
        group.copy(fixed = HistoryStat(test, stats) :: group.fixed)
      } else if (ratePrevious > last) {
        group.copy(better = HistoryStat(test, stats) :: group.better)
      } else if (ratePrevious < last) {
        group.copy(worse = HistoryStat(test, stats) :: group.worse)
      } else {
        group.copy(noChange = HistoryStat(test, stats) :: group.noChange)
      }
    } else {
      stats.headOption.map { firstStat =>
        if (firstStat.failureRate > 0) {
          group.copy(newCases = HistoryStat(test, stats) :: group.newCases)
        } else {
          group.copy(good = HistoryStat(test, stats) :: group.good)
        }
      }.getOrElse(group)
    }
  }
}
