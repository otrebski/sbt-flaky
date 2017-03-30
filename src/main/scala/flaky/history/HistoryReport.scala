package flaky.history

import flaky.{FlakyTest, FlakyTestReport, Test}

import scala.collection.immutable

class HistoryReport {

  case class HistoricalRun(date: String, report: FlakyTestReport)

  case class HistoryData(historicalRuns: List[HistoricalRun]) {
    def flaky(): Unit = {
      val flakyTests: List[Test] = historicalRuns
        .flatMap(_.report.flakyTests)
        .map(f => f.test)

      val r: immutable.Seq[FlakyTest] = for {
        flakyTest <- flakyTests
        historicalRun <- historicalRuns
        report <- historicalRun.report.flakyTests
        ft <- report.failedRuns if ft.test == flakyTest
        count <- ft.
      } yield report

    }
  }

}
