package flaky.history

import flaky._
import org.scalatest.{Matchers, WordSpec}


class HistoryReportSpec extends WordSpec with Matchers {
  val worseStat: List[Stat] = List(Stat("0", 0, 1), Stat("1", 2, 10), Stat("2", 3, 10))
  val improvingStat: List[Stat] = List(Stat("0", 0, 10), Stat("1", 4, 10), Stat("2", 3, 10))
  val goodStat: List[Stat] = List(Stat("0", 0, 10), Stat("1", 0, 10), Stat("2", 0, 10))
  val fixedStat: List[Stat] = List(Stat("0", 0, 10), Stat("1", 1, 10), Stat("2", 0, 10))
  val newCaseStat: List[Stat] = List(Stat("0", 0, 10), Stat("1", 0, 10), Stat("2", 1, 10))
  val noChange: List[Stat] = List(Stat("0", 0, 10), Stat("1", 1, 10), Stat("2", 1, 10))
  val test = Test("a", "b")
  "HistoryReport" should {
    "group worse test result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, worseStat)
      result shouldBe Grouped(worse = List(HistoryStat(test, worseStat)))
    }
    "group improving test result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, improvingStat)
      result shouldBe Grouped(better = List(HistoryStat(test, improvingStat)))
    }
    "group good test result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, goodStat)
      result shouldBe Grouped(good = List(HistoryStat(test, goodStat)))
    }
    "group fixed test result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, fixedStat)
      result shouldBe Grouped(fixed = List(HistoryStat(test, fixedStat)))
    }
    "group new cases test result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, newCaseStat)
      result shouldBe Grouped(newCases = List(HistoryStat(test, newCaseStat)))
    }
    "group new cases test result if having only one result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, List(Stat("0", 1, 10)))
      result shouldBe Grouped(newCases = List(HistoryStat(test, List(Stat("0", 1, 10)))))
    }
    "group no changed test result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, noChange)
      result shouldBe Grouped(noChange = List(HistoryStat(test, noChange)))
    }
    "group good test result if having only one result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, List(Stat("0", 0, 10)))
      result shouldBe Grouped(good = List(HistoryStat(test, List(Stat("0", 0, 10)))))
    }

    "group tests summary" in {
      val goodTest = Test("good", "test")
      val fixedTest = Test("fixed", "test")
      val worseTest = Test("worse", "fixed")
      val historyStat: List[TestSummary] = List(
        TestSummary(goodTest, Stat("0", 0, 1)),
        TestSummary(goodTest, Stat("1", 0, 1)),
        TestSummary(goodTest, Stat("2", 0, 1)),

        TestSummary(fixedTest, Stat("0", 0, 1)),
        TestSummary(fixedTest, Stat("1", 1, 10)),
        TestSummary(fixedTest, Stat("2", 0, 1)),

        TestSummary(worseTest, Stat("0", 0, 10)),
        TestSummary(worseTest, Stat("1", 1, 10)),
        TestSummary(worseTest, Stat("2", 2, 10))
      )

      val grouped = HistoryReport.grouped(historyStat)

      grouped.better shouldBe List.empty
      grouped.fixed shouldBe List(HistoryStat(fixedTest, List(Stat("0", 0, 1), Stat("1", 1, 10), Stat("2", 0, 1))))
      grouped.good shouldBe List(HistoryStat(goodTest, List(Stat("0", 0, 1), Stat("1", 0, 1), Stat("2", 0, 1))))
      grouped.newCases shouldBe List.empty
      grouped.noChange shouldBe List.empty
      grouped.worse shouldBe List(HistoryStat(worseTest, List(Stat("0", 0, 1), Stat("1", 1, 10), Stat("2", 2, 10))))
    }

    "group historical runs" in {
      val runs1 = List(
        TestRun("1", List(TestCase("1", Test("t", "t1")), TestCase("1", Test("t", "t2")))),
        TestRun("2", List(TestCase("2", Test("t", "t1")), TestCase("2", Test("t", "t2"))))
      )
      val flakyTests1: List[FlakyTest] = List.empty[FlakyTest]


      val historicalRun1 = HistoricalRun(HistoryReportDescription(1L, Some("A")), FlakyTestReport("", TimeDetails(0, 0), runs1, flakyTests1))

      val runs2 = List(
        TestRun("1", List(TestCase("1", Test("t", "t1")), TestCase("1", Test("t", "t2")))),
        TestRun("2", List(TestCase("2", Test("t", "t1")), TestCase("2", Test("t", "t2"), failureDetails = Some(FailureDetails("msg", "type", "stacktrace")))))
      )
      val flakyTests2: List[FlakyTest] = List(FlakyTest(Test("t", "t2"), 2, List(TestCase("1", Test("t", "t2"), failureDetails = Some(FailureDetails("msg", "type", "stacktrace"))))))
      val historicalRun2 = HistoricalRun(HistoryReportDescription(2L, Some("A")), FlakyTestReport("", TimeDetails(0, 0), runs2, flakyTests2))

      val grouped = HistoryReport("Project", "2014", List(historicalRun1, historicalRun2)).grouped()
      grouped.better shouldBe List.empty
      grouped.fixed shouldBe List.empty
      grouped.newCases shouldBe List(HistoryStat(Test("t", "t2"), List(Stat("1", 0, 1), Stat("2", 1, 2))))
      grouped.good shouldBe List(HistoryStat(Test("t", "t1"), List(Stat("1", 0, 1), Stat("2", 0, 1))))
      grouped.noChange shouldBe List.empty
      grouped.worse shouldBe List.empty

    }
  }


}
