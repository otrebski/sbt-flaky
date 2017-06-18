package flaky

import org.scalatest.{Matchers, WordSpec}

class FlakyTestReportSpec extends WordSpec with Matchers {

  private val test = Test("", "")
  private val timeDetails = TimeDetails(0, 0)
  private val someFailureDetails = Some(FailureDetails("", "", ""))
  private val testCase = TestCase("", test, 0, someFailureDetails)
  private val flakyTest = FlakyTest(
    test,
    10,
    List(
      testCase
    )
  )
  private val testRuns: List[TestRun] = (0 until 10).map(i => TestRun(s"a$i", List(testCase))).toList

  "FlakyTestReportSpec" should {

    "successProbabilityPercent with one test" in {

      val report: FlakyTestReport = FlakyTestReport(
        "",
        timeDetails,
        testRuns,
        List(flakyTest)
      )
      report.successProbabilityPercent() shouldBe 90.0
    }

    "successProbabilityPercent with two tests" in {

      val report: FlakyTestReport = FlakyTestReport(
        "",
        timeDetails,
        testRuns,
        List(flakyTest, flakyTest, FlakyTest(test, 10, List.empty))
      )

      val fl: Float = report.successProbabilityPercent()
      fl.toDouble shouldBe (81.0 +- 2)
    }

    "successProbabilityPercent with no flaky tests" in {

      val report: FlakyTestReport = FlakyTestReport(
        "",
        timeDetails,
        testRuns,
        List(FlakyTest(test, 10, List.empty))
      )
      report.successProbabilityPercent() shouldBe 100.0
    }

    "successProbabilityPercent with  \"(It is not a test)\"" in {
      val thisIsNotATest: Test = Test("A", "(It is not a test)")

      val report: FlakyTestReport = FlakyTestReport(
        "",
        timeDetails,
        testRuns,
        List(
          FlakyTest(thisIsNotATest, 1, List.empty),
          FlakyTest(test, 9, List.empty)
        )
      )
      report.successProbabilityPercent() shouldBe 90.0
    }
  }
}
