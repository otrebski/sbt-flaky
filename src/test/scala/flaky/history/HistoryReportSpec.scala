package flaky.history

import flaky._
import org.scalatest.{FunSuite, Matchers, WordSpec}


class HistoryReportSpec extends WordSpec with Matchers {
  val worseStat: List[Stat] = List(Stat("0", 0), Stat("1", 0.2f), Stat("2", 0.3f))
  val improvingStat: List[Stat] = List(Stat("0", 0), Stat("1", 0.4f), Stat("2", 0.3f))
  val goodStat: List[Stat] = List(Stat("0", 0), Stat("1", 0f), Stat("2", 0f))
  val fixedStat: List[Stat] = List(Stat("0", 0), Stat("1", 0.1f), Stat("2", 0f))
  val newCaseStat: List[Stat] = List(Stat("0", 0), Stat("1", 0f), Stat("2", 0.1f))
  val noChange: List[Stat] = List(Stat("0", 0), Stat("1", 0.1f), Stat("2", 0.1f))
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
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, List(Stat("0", 0.1f)))
      result shouldBe Grouped(newCases = List(HistoryStat(test, List(Stat("0", 0.1f)))))
    }
    "group no changed test result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, noChange)
      result shouldBe Grouped(noChange = List(HistoryStat(test, noChange)))
    }
    "group good test result if having only one result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, List(Stat("0", 0)))
      result shouldBe Grouped(good = List(HistoryStat(test, List(Stat("0", 0)))))
    }

    "group tests summary" in {
      val goodTest = Test("good", "test")
      val fixedTest = Test("fixed", "test")
      val worseTest = Test("worse", "fixed")
      val historyStat: List[TestSummary] = List(
        TestSummary(goodTest, Stat("0", 0)),
        TestSummary(goodTest, Stat("1", 0)),
        TestSummary(goodTest, Stat("2", 0)),

        TestSummary(fixedTest, Stat("0", 0)),
        TestSummary(fixedTest, Stat("1", 0.1f)),
        TestSummary(fixedTest, Stat("2", 0)),

        TestSummary(worseTest, Stat("0", 0)),
        TestSummary(worseTest, Stat("1", 0.1f)),
        TestSummary(worseTest, Stat("2", 0.2f))
      )

      val grouped = HistoryReport.grouped(historyStat)

      grouped.better shouldBe List.empty
      grouped.fixed shouldBe List(HistoryStat(fixedTest, List(Stat("0", 0), Stat("1", 0.1f), Stat("2", 0))))
      grouped.good shouldBe List(HistoryStat(goodTest, List(Stat("0", 0), Stat("1", 0), Stat("2", 0))))
      grouped.newCases shouldBe List.empty
      grouped.noChange shouldBe List.empty
      grouped.worse shouldBe List(HistoryStat(worseTest, List(Stat("0", 0), Stat("1", 0.1f), Stat("2", 0.2f))))
    }

    "group historical runs" in {
      val runs1 = List(
        TestRun("1", List(TestCase("1", Test("t", "t1")), TestCase("1", Test("t", "t2")))),
        TestRun("2", List(TestCase("2", Test("t", "t1")), TestCase("1", Test("t", "t2"))))
      )
      val flakyTests1: List[FlakyTest] = List.empty[FlakyTest]

      val historicalRun1 = HistoricalRun("1", FlakyTestReport("", TimeDetails(0, 0), runs1, flakyTests1))

      val runs2 = List(
        TestRun("1", List(TestCase("1", Test("t", "t1")), TestCase("1", Test("t", "t2")))),
        TestRun("2", List(TestCase("2", Test("t", "t1")), TestCase("1", Test("t", "t2"), failureDetails = Some(FailureDetails("msg", "type", "stacktrace")))))
      )
      val flakyTests2: List[FlakyTest] = List(FlakyTest(Test("t", "t2"), 2, List(TestCase("1", Test("t", "t2"), failureDetails = Some(FailureDetails("msg", "type", "stacktrace"))))))
      val historicalRun2 = HistoricalRun("2", FlakyTestReport("", TimeDetails(0, 0), runs2, flakyTests2))

      val grouped = HistoryReport("", List(historicalRun1, historicalRun2)).grouped()
      grouped.better shouldBe List.empty
      grouped.fixed shouldBe List.empty
      grouped.newCases shouldBe List(HistoryStat(Test("t","t2"), List(Stat("1", 0), Stat("2", 50f))))
      grouped.good shouldBe List(HistoryStat(Test("t","t1"), List(Stat("1", 0), Stat("2", 0))))
      grouped.noChange shouldBe List.empty
      grouped.worse shouldBe List.empty

    }
  }


}
