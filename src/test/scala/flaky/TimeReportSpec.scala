package flaky

import org.scalatest.{Matchers, WordSpec}

class TimeReportSpec extends WordSpec with Matchers {
  "TimeReport" should {

    "estimate how long 5 more runs will take" in {
      TimeReport(10, 60*1000L).estimate(5) shouldBe "0m 30s"
    }

    "estimate how many runs will do in 60 seconds" in {
      TimeReport(5, 30*1000L).estimateCountIn(60*1000L) shouldBe "10 times"
    }

    "format time less than minute to human readable format" in {
      TimeReport.formatSeconds(4) shouldBe "0m 4s"
    }

    "format time more than minute to human readable format" in {
      TimeReport.formatSeconds(64) shouldBe "1m 4s"
    }

    "format minute to human readable format" in {
      TimeReport.formatSeconds(60) shouldBe "1m 0s"
    }

  }
}
