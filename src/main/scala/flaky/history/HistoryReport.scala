package flaky.history

import flaky._

import scala.collection.immutable.Seq

case class HistoricalRun(date: String, report: FlakyTestReport)

case class Stat(date: String, failureRate: Float)

case class TestSummary(test: Test, stat: Stat)

case class HistoryStat(test: Test, stats: List[Stat] = List.empty[Stat])

case class HistoryData(historicalRuns: List[HistoricalRun]) {

  def testStats(): List[TestSummary] = {
    val flakyTests: List[Test] = historicalRuns
      .flatMap(_.report.flakyTests)
      .map(f => f.test)
      .distinct

    val r: Seq[TestSummary] = for {
      flakyTest <- flakyTests
      historicalRun <- historicalRuns
      flakyTestData <- historicalRun.report.flakyTests if flakyTestData.test == flakyTest
    } yield TestSummary(flakyTest, Stat(historicalRun.date, flakyTestData.failurePercent()))

    r.toList
  }
}

case class Grouped(
                    good: List[HistoryStat] = List.empty[HistoryStat],
                    newCases: List[HistoryStat] = List.empty[HistoryStat],
                    fixed: List[HistoryStat] = List.empty[HistoryStat],
                    better: List[HistoryStat] = List.empty[HistoryStat],
                    noChange: List[HistoryStat] = List.empty[HistoryStat],
                    worse: List[HistoryStat] = List.empty[HistoryStat])

case class HistoryReport(date: String, historyStat: List[TestSummary]) {

  def grouped(): Grouped = {
    historyStat
      .groupBy(_.test)
      .foldLeft(Grouped()) { (group, tl) =>
        val test = tl._1
        val stats = tl._2.sortBy(_.stat.date).map(_.stat)
        HistoryReport.groupTestResult(group, test, stats)
      }
  }
}


object HistoryReport {
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
      group.copy(newCases = HistoryStat(test, stats) :: group.newCases)
    }
  }
}
