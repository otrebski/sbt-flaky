package flaky

import java.io.File

import org.scalatest.{Matchers, WordSpec}

import scala.collection.immutable.Seq
import scala.xml.XML

class FlakySpec extends WordSpec with Matchers {

  private val flakyReportDirSuccessful = new File("./src/test/resources/flakyTestRuns/successful/target/flaky-report/")
  private val successfulReport = Flaky.createReport("P1", TimeDetails(0, 100), Seq("1", "2"), flakyReportDirSuccessful)
  private val flakyReportDirWithFailures = new File("./src/test/resources/flakyTestRuns/withFailures/target/flaky-report/")
  private val failedReport = Flaky.createReport("P1", TimeDetails(0, 100), Seq("1", "2", "3"), flakyReportDirWithFailures)
  private val flakyReportDirAllFailures = new File("./src/test/resources/flakyTestRuns/allFailures/target/flaky-test-reports/")
  private val flakyReportAllFailures = Flaky.createReport("P1", TimeDetails(0, 100), Seq("1", "2", "3", "4", "5"), flakyReportDirAllFailures)

  "Flaky" should {

    "create report based on 2 successful runs" in {
      successfulReport.flakyTests.size shouldBe 9
      successfulReport.flakyTests.head.totalRun shouldBe 2
      successfulReport.flakyTests should contain(FlakyTest("ExampleSpec", "A Stack should fail randomly often", 2, List.empty))
      successfulReport.flakyTests should contain(FlakyTest("TestKitUsageSpec", "A ForwardingActor should Forwards in a 2 huge chains", 2, List.empty))
    }

    "create report based on 1 run and dir contains 2" in {
      val successfulReport = Flaky.createReport("P1", TimeDetails(0, 100), Seq("1"), flakyReportDirSuccessful)
      successfulReport.flakyTests.size shouldBe 9
      successfulReport.flakyTests.head.totalRun shouldBe 1
    }

    "create report based on run with failures" in {
      failedReport.flakyTests.size shouldBe 9
      failedReport.flakyTests.find(_.test == "A Stack should fail sometimes").map(_.failures()) shouldBe Some(1)
      failedReport.flakyTests.find(_.test == "A ForwardingActor should Forwards in a 2 huge chains").map(_.failures()) shouldBe Some(1)
    }

    "find failed tests in empty reports" in {
      Flaky.findFlakyTests(List.empty[TestRun]) shouldBe 'empty
    }

    "find failed tests in successful reports" in {
      val runs: Seq[TestRun] = List(
        TestRun("run1", List(
          TestCase("run1", "t", "t1", 1f, None),
          TestCase("run1", "t", "t2", 1f, None))),
        TestRun("run2", List(
          TestCase("run2", "t", "t1", 1f, None),
          TestCase("run2", "t", "t2", 1f, None)))
      )
      val findFlakyTests: Seq[FlakyTest] = Flaky.findFlakyTests(runs.toList)
      findFlakyTests.toSet shouldBe Set(
        FlakyTest("t", "t1", 2, List.empty[TestCase]),
        FlakyTest("t", "t2", 2, List.empty[TestCase]))
    }

    "find failed tests in failed reports" in {
      val failedTestCase = TestCase("run2", "t", "t2", 1f, Some(FailureDetails("msg", "ftype", "stacktrace")))
      val runs: Seq[TestRun] = List(
        TestRun("run1", List(
          TestCase("run1", "t", "t1", 1f, None),
          TestCase("run1", "t", "t2", 1f, None))),
        TestRun("run2", List(
          TestCase("run2", "t", "t1", 1f, None),
          failedTestCase))
      )

      val findFlakyTests: Seq[FlakyTest] = Flaky.findFlakyTests(runs.toList)
      findFlakyTests.toSet shouldBe Set(
        FlakyTest("t", "t1", 2, List.empty[TestCase]),
        FlakyTest("t", "t2", 2, List(failedTestCase)))
    }

    "detect failures in reports dir" in {
      val file = new File(flakyReportDirWithFailures, "1")
      Flaky.isFailed(file) shouldBe true
    }

    "detect success in reports dir" in {
      val file = new File(flakyReportDirWithFailures, "2")
      Flaky.isFailed(file) shouldBe false
    }

    "parse successful junit xml report" in {
      val expected = Set(
        TestCase("run1", "ExampleSpec", "A Stack should pop values in last-in-first-out order", 0.034f, None),
        TestCase("run1", "ExampleSpec", "A Stack should throw NoSuchElementException if an empty stack is popped", 0.005f, None),
        TestCase("run1", "ExampleSpec", "A Stack should fail sometimes", 0.002f, None),
        TestCase("run1", "ExampleSpec", "A Stack should fail randomly often", 0.005f, None),
        TestCase("run1", "ExampleSpec", "A Stack should fail randomly", 0.001f, None),
        TestCase("run1", "ExampleSpec", "A Stack should fail randomly sometimes", 0.0f, None)
      )

      val file = new File("src/test/resources/testReports/successful.xml")
      val testCases: Seq[TestCase] = Flaky.parseJunitXmlReport("run1", XML.loadFile(file))

      testCases.toSet shouldBe expected
    }

    "parse junit xml report with failure" in {
      val failureDetails = FailureDetails("false was not true", "org.scalatest.exceptions.TestFailedException", "stacktrace")
      val expected = Set(
        TestCase("run1", "ExampleSpec", "A Stack should pop values in last-in-first-out order", 0.018f, None),
        TestCase("run1", "ExampleSpec", "A Stack should throw NoSuchElementException if an empty stack is popped", 0.003f, None),
        TestCase("run1", "ExampleSpec", "A Stack should fail sometimes", 0.016f, Some(failureDetails)),
        TestCase("run1", "ExampleSpec", "A Stack should fail randomly often", 0.004f, None),
        TestCase("run1", "ExampleSpec", "A Stack should fail randomly", 0.0f, None),
        TestCase("run1", "ExampleSpec", "A Stack should fail randomly sometimes", 0.0f, None)
      )

      val file = new File("src/test/resources/testReports/withFailure.xml")
      val testCases: Seq[TestCase] = Flaky.parseJunitXmlReport("run1", XML.loadFile(file))

      testCases.toSet shouldBe expected
    }

    "parse junit xml report with error" in {
      val failureDetails = FailureDetails("!", "java.lang.OutOfMemoryError", "java.lang.OutOfMemoryError: !")
      val expected = Seq(TestCase("run1", "flaky.FlakySpec", "testName", 1.0f, Some(failureDetails)))

      val file = new File("src/test/resources/testReports/withError.xml")
      val testCases: Seq[TestCase] = Flaky.parseJunitXmlReport("run1", XML.loadFile(file))

      testCases shouldBe expected
    }

  }
}
