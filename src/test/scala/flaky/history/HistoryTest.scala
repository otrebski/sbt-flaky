package flaky.history

import java.io.File

import org.scalatest.{Matchers, WordSpec}

class HistoryTest extends WordSpec with Matchers {

  val fileWithDescriptor = "20170516-072750.zip"
  val fileWithoutDescriptor = "20170516-072825.zip"
  val dirWithReports = new File("./src/test/resources/history")

  "HistoryTest" should {

    "loadHistory with descriptor" in {
      val historicalRun: HistoricalRun = History.loadHistory.apply(new File(dirWithReports, fileWithDescriptor))
      historicalRun.historyReportDescription shouldBe HistoryReportDescription(123456L, Some("abcdefg"))
    }
    "loadHistory without descriptor" in {
      val historicalRun: HistoricalRun = History.loadHistory.apply(new File(dirWithReports, fileWithoutDescriptor))
      historicalRun.historyReportDescription shouldBe HistoryReportDescription(1494912505000L, None)
    }

  }
}
