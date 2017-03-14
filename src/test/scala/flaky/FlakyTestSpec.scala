package flaky

import java.io.File

import org.scalatest.{Matchers, WordSpec}

class FlakyTestSpec extends WordSpec with Matchers {

  private val flakyReportDirAllFailures = new File("./src/test/resources/flakyTestRuns/allFailures/target/flaky-test-reports/")
  private val flakyReportAllFailures = Flaky.createReport("P1", TimeDetails(0, 100), Seq("1", "2", "3","4","5"), flakyReportDirAllFailures)


  "FlakyTest" should {
    "group by stacktrace with ignoring message" in {
      val head = flakyReportAllFailures.flakyTests.head
      val groupByStacktrace = head.groupByStacktrace()
      groupByStacktrace.size shouldBe 2
    }
  }

}
