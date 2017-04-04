package flaky.history

import flaky._

import scala.collection.immutable.Seq

case class HistoricalRun(date: String, report: FlakyTestReport)

case class HistoryStat(test: Test, date: String, failureRate: Float)

case class HistoryData(historicalRuns: List[HistoricalRun]) {
  def flakyReport(): String = {
    val flakyTests: List[Test] = historicalRuns
      .flatMap(_.report.flakyTests)
      .map(f => f.test)
      .distinct

    val r: Seq[HistoryStat] = for {
      flakyTest <- flakyTests
      historicalRun <- historicalRuns
      flakyTestData <- historicalRun.report.flakyTests if flakyTestData.test == flakyTest
    } yield HistoryStat(flakyTest, historicalRun.date, flakyTestData.failurePercent())

    r.mkString(",")
  }
}


class HistoryReport {


}

object HistoryReport {
  def main(args: Array[String]): Unit = {
    val runs1: Seq[TestRun] = List(
      TestRun("run1", List(TestCase("run1", Test("t", "t1")), TestCase("run1", Test("t", "t2")))),
      TestRun("run2", List(TestCase("run2", Test("t", "t1"), 1f, Some(FailureDetails("msg", "ftype", "stacktrace"))), TestCase("run2", Test("t", "t2")))),
      TestRun("run3", List(TestCase("run3", Test("t", "t1")), TestCase("run3", Test("t", "t2"))))
    )
    val historicalRun1 = HistoricalRun("1", FlakyTestReport("A", TimeDetails(0, 1), runs1.toList, Flaky.findFlakyTests(runs1.toList)))


    val runs2: Seq[TestRun] = List(
      TestRun("run1", List(TestCase("run1", Test("t", "t1")), TestCase("run1", Test("t", "t2")))),
      TestRun("run2", List(TestCase("run2", Test("t", "t1")), TestCase("run2", Test("t", "t2")))),
      TestRun("run3", List(TestCase("run3", Test("t", "t1")), TestCase("run3", Test("t", "t2"))))
    )
    val historicalRun2 = HistoricalRun("2", FlakyTestReport("A", TimeDetails(0, 1), runs2.toList, Flaky.findFlakyTests(runs2.toList)))

    val historyData = HistoryData(List(historicalRun1, historicalRun2))

    val report = historyData.flakyReport()
    println(report)

  }

}
