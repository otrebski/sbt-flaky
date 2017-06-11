package flaky

import org.scalatest.{Matchers, WordSpec}

class FlakyTestReportSpec extends WordSpec with Matchers {

  val test = Test("", "")
  val timeDetails = TimeDetails(0, 0)
  val someFailureDetails = Some(FailureDetails("", "", ""))
  val flakyTest = FlakyTest(
    test,
    10,
    List(
      TestCase("", test, 0, someFailureDetails)
    )
  )

  "FlakyTestReportSpec" should {

    "successProbabilityPercent with one test" in {

      val report: FlakyTestReport = FlakyTestReport(
        "",
        timeDetails,
        List.empty[TestRun],
        List(flakyTest)
      )
      report.successProbabilityPercent() shouldBe 90.0
    }

    "successProbabilityPercent with two tests" in {

      val report: FlakyTestReport = FlakyTestReport(
        "",
        timeDetails,
        List.empty[TestRun],
        List(flakyTest, flakyTest, FlakyTest(test, 10, List.empty))
      )

      val fl: Float = report.successProbabilityPercent()
      fl.toDouble shouldBe (81.0 +- 2)
    }

    "successProbabilityPercent with no flaky tests" in {

      val report: FlakyTestReport = FlakyTestReport(
        "",
        timeDetails,
        List.empty[TestRun],
        List(FlakyTest(test, 10, List.empty))
      )
      report.successProbabilityPercent() shouldBe 100.0
    }

  }
}
