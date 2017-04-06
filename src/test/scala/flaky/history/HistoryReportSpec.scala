package flaky.history

import flaky.{Test, history}
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
    "group no changed test result" in {
      val result: Grouped = HistoryReport.groupTestResult(Grouped(), test, noChange)
      result shouldBe Grouped(noChange = List(HistoryStat(test, noChange)))
    }

    "" in {
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
      val report = HistoryReport("Some date", historyStat)

      val grouped = report.grouped()
      grouped.better shouldBe List.empty
      grouped.fixed shouldBe List(HistoryStat(fixedTest, List(Stat("0", 0), Stat("1", 0.1f), Stat("2", 0))))
      grouped.good shouldBe List(HistoryStat(goodTest, List(Stat("0", 0), Stat("1", 0), Stat("2", 0))))
      grouped.newCases shouldBe List.empty
      grouped.noChange shouldBe List.empty
      grouped.worse shouldBe List(HistoryStat(worseTest, List(Stat("0", 0), Stat("1", 0.1f), Stat("2", 0.2f))))
    }
  }

}
