package flaky

import java.io.File

import org.scalatest.{Matchers, WordSpec}

class FlakyTestSpec extends WordSpec with Matchers {

  private val flakyReportDirAllFailures = new File("./src/test/resources/flakyTestRuns/allFailures/target/flaky-test-reports/")
  private val flakyReportAllFailures = Flaky.createReport(
    "P1", 
    TimeDetails(0, 100), 
    List("1", "2", "3","4","5"), 
    flakyReportDirAllFailures
  )


  "FlakyTest" should {
    "group by stacktrace with ignoring message" in {
      val flakyTests = flakyReportAllFailures.flakyTests     
      val flakyTest = flakyTests(1) //Maybe it should be sorted?      
      val groupByStacktrace = flakyTest.groupByStacktrace()
      groupByStacktrace.size shouldBe 2
    }
  }

}
