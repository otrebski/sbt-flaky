package flaky

import java.io.File

import org.scalatest.{Matchers, WordSpec}

import scala.collection.immutable.{Iterable, Seq}
import scala.xml.XML

class FlakySpec extends WordSpec with Matchers {

  private val flakyReportDirSuccessful: File = new File("./src/test/resources/flakyTestRuns/successful/target/flaky-report/")
  private val successfulReport: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2"), flakyReportDirSuccessful)
  private val flakyReportDirWithFailures: File = new File("./src/test/resources/flakyTestRuns/withFailures/target/flaky-report/")
  private val failedReport: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2", "3"), flakyReportDirWithFailures)
  private val flakyReportDirAllFailures: File = new File("./src/test/resources/flakyTestRuns/allFailures/target/flaky-test-reports/")
  private val flakyReportAllFailures: FlakyTestReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1", "2", "3", "4", "5"), flakyReportDirAllFailures)

  "Flaky" should {

    "create report based on 2 successful runs" in {
      successfulReport.flakyTests.size shouldBe 9
      successfulReport.flakyTests.head.totalRun shouldBe 2
      successfulReport.flakyTests should contain(FlakyTest(Test("ExampleSpec", "A Stack should fail randomly often"), 2, List.empty))
      successfulReport.flakyTests should contain(FlakyTest(Test("TestKitUsageSpec", "A ForwardingActor should Forwards in a 2 huge chains"), 2, List.empty))
    }

    "create report based on 1 run and dir contains 2" in {
      val successfulReport = Flaky.createReport("P1", TimeDetails(0, 100), List("1"), flakyReportDirSuccessful)
      successfulReport.flakyTests.size shouldBe 9
      successfulReport.flakyTests.head.totalRun shouldBe 1
    }

    "create report based on run with failures" in {
      failedReport.flakyTests.size shouldBe 9
      failedReport.flakyTests.find(_.test.test == "A Stack should fail sometimes").map(_.failures()) shouldBe Some(1)
      failedReport.flakyTests.find(_.test.test == "A ForwardingActor should Forwards in a 2 huge chains").map(_.failures()) shouldBe Some(1)
    }

    "find failed tests in empty reports" in {
      Flaky.findFlakyTests(List.empty[TestRun]) shouldBe 'empty
    }

    "find failed tests in successful reports" in {
      val runs: Seq[TestRun] = List(
        TestRun("run1", List(
          TestCase("run1",Test( "t", "t1"), 1f, None),
          TestCase("run1",Test( "t", "t2"), 1f, None))),
        TestRun("run2", List(
          TestCase("run2", Test("t", "t1"), 1f, None),
          TestCase("run2", Test("t", "t2"), 1f, None)))
      )
      val findFlakyTests: Seq[FlakyTest] = Flaky.findFlakyTests(runs.toList)
      findFlakyTests.toSet shouldBe Set(
        FlakyTest(Test("t", "t1"), 2, List.empty[TestCase]),
        FlakyTest(Test("t", "t2"), 2, List.empty[TestCase]))
    }

    "find failed tests in failed reports" in {
      val failedTestCase = TestCase("run2", Test("t", "t2"), 1f, Some(FailureDetails("msg", "ftype", "stacktrace")))
      val runs: Seq[TestRun] = List(
        TestRun("run1", List(
          TestCase("run1", Test("t", "t1"), 1f, None),
          TestCase("run1", Test("t", "t2"), 1f, None))),
        TestRun("run2", List(
          TestCase("run2", Test("t", "t1"), 1f, None), failedTestCase))
      )

      val findFlakyTests: Seq[FlakyTest] = Flaky.findFlakyTests(runs.toList)
      findFlakyTests.toSet shouldBe Set(
        FlakyTest(Test("t", "t1"), 2, List.empty[TestCase]),
        FlakyTest(Test("t", "t2"), 2, List(failedTestCase)))
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
        TestCase("run1", Test("ExampleSpec", "A Stack should pop values in last-in-first-out order"), 0.034f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should throw NoSuchElementException if an empty stack is popped"), 0.005f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should fail sometimes"), 0.002f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should fail randomly often"), 0.005f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should fail randomly"), 0.001f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should fail randomly sometimes"), 0.0f, None)
      )

      val file = new File("src/test/resources/testReports/successful.xml")
      val testCases: Seq[TestCase] = Flaky.parseJunitXmlReport("run1", XML.loadFile(file))

      testCases.toSet shouldBe expected
    }

    "parse junit xml report with failure" in {
      val failureDetails = FailureDetails("false was not true", "org.scalatest.exceptions.TestFailedException", "stacktrace")
      val expected = Set(
        TestCase("run1", Test("ExampleSpec", "A Stack should pop values in last-in-first-out order"), 0.018f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should throw NoSuchElementException if an empty stack is popped"), 0.003f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should fail sometimes"), 0.016f, Some(failureDetails)),
        TestCase("run1", Test("ExampleSpec", "A Stack should fail randomly often"), 0.004f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should fail randomly"), 0.0f, None),
        TestCase("run1", Test("ExampleSpec", "A Stack should fail randomly sometimes"), 0.0f, None)
      )

      val file = new File("src/test/resources/testReports/withFailure.xml")
      val testCases: Seq[TestCase] = Flaky.parseJunitXmlReport("run1", XML.loadFile(file))

      testCases.toSet shouldBe expected
    }

    "parse junit xml report with error" in {
      val failureDetails = FailureDetails("!", "java.lang.OutOfMemoryError", "java.lang.OutOfMemoryError: !")
      val expected = Seq(TestCase("run1", Test("flaky.FlakySpec", "testName"), 1.0f, Some(failureDetails)))

      val file = new File("src/test/resources/testReports/withError.xml")
      val testCases: Seq[TestCase] = Flaky.parseJunitXmlReport("run1", XML.loadFile(file))

      testCases shouldBe expected
    }

    "group flaky cases in successful report" in {
      val grouped = successfulReport.groupFlakyCases()
      grouped shouldBe Map.empty[String, Iterable[List[FlakyCase]]]
    }

    "group flaky cases in report with failure" in {
      val grouped = flakyReportAllFailures.groupFlakyCases()
      val flakyCases = List(
        FlakyCase(
          Test("tests.DateFormattingTest","formatParallelTest"),
          List("1", "3", "4", "5"),
          Some("expected:<00:00:00.00[_]> but was:<00:00:00.00[_]>"),
          "\tat tests.DateFormattingTest.formatParallelTest(DateFormattingTest.java:27)",
        Set("expected:<00:00:00.00[_]> but was:<00:00:00.00[_]>")),
        FlakyCase(
          Test("tests.DateFormattingTest","formatParallelTest"),
          List("2"),
          Some("expected:<00:00:00.00[0]> but was:<00:00:00.00[3]>"),
          "\tat tests.DateFormattingTest.formatParallelTest(DateFormattingTest.java:25)",
          Set("expected:<00:00:00.00[_]> but was:<00:00:00.00[_]>"))
      )
      grouped shouldBe Map("tests.DateFormattingTest" -> flakyCases)
    }

  }
}
