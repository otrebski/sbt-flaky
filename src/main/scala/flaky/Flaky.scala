package flaky

import java.io.File

import scala.language.postfixOps
import scala.util.Try
import scala.xml.{Elem, NodeSeq, XML}

case class TestCase(runName: String,
                    clazz: String,
                    test: String,
                    time: Float,
                    failureDetails: Option[FailureDetails] = None
                   )

case class FailureDetails(message: String, ftype: String, stacktrace: String) {
  def withoutStacktraceMessage(): FailureDetails = {
    val newStacktraceWithoutMessage = stacktrace.substring(stacktrace.indexOf("\n"))
    copy(stacktrace = newStacktraceWithoutMessage)
  }

  def firstNonAssertStacktrace(): Option[String] = {
    stacktrace
      .lines
      .filter(_.startsWith("\tat"))
      .filter(!_.startsWith("\tat org.junit"))
      .filter(!_.startsWith("\tat org.testng"))
      .filter(!_.startsWith("\tat org.scalatest"))
      .filter(!_.startsWith("\tat java."))
      .filter(!_.startsWith("\tat scala."))
      .filter(!_.startsWith("\tat akka."))
      .find(_ => true)
  }
}

case class TestRun(
                    name: String,
                    testCases: List[TestCase]
                  )

case class FlakyTest(
                      clazz: String,
                      test: String,
                      totalRun: Int,
                      failedRuns: List[TestCase]
                    ) {
  def failures(): Int = failedRuns.size

  def groupByStacktrace(): Iterable[List[TestCase]] = {
    failedRuns.map { tc =>
      tc.copy(failureDetails = tc.failureDetails.map(_.withoutStacktraceMessage()))
    }.groupBy(_.failureDetails.map(_.stacktrace))
      .values
  }
}

case class TimeDetails(start: Long, end: Long) {
  def duration(): Long = end - start
}

case class FlakyTestReport(projectName: String, timeDetails: TimeDetails, testRuns: List[TestRun], flakyTests: List[FlakyTest])

object Flaky {

  def parseJunitXmlReport(runName: String, xml: Elem): List[TestCase] = {
    val testCases = xml \\ "testcase"
    testCases.map { testcase =>
      val className = testcase \ "@classname"
      val name = testcase \ "@name"
      val time = testcase \ "@time"
      val fail: NodeSeq = testcase \ "failure"
      val error = testcase \ "error"

      val failureDetails: Option[FailureDetails] = fail.headOption
        .orElse(error.headOption)
        .map { head =>
          FailureDetails(
            head \ "@message" text,
            head \ "@type" text,
            head.text)
        }

      TestCase(
        runName,
        className.text,
        name.text,
        time.text.toFloat,
        failureDetails
      )
    }.toList
  }

  def processFolder(dir: File): List[TestCase] = {
    val f = dir.listFiles.toList
    f
      .map(_.getAbsolutePath)
      .filter(_.endsWith("xml"))
      .map(x => Try {
        XML.loadFile(x)
      })
      .filter(_.isSuccess)
      .map(_.get)
      .flatMap { xml => parseJunitXmlReport(dir.getName, xml) }
  }

  def findFlakyTests(list: List[TestRun]): List[FlakyTest] = {
    case class Test(clazz: String, test: String)

    val map = list.flatMap(tr => tr.testCases)
      .groupBy(tc => Test(tc.clazz, tc.test))

    map.keySet.map { key =>
      val testCases: List[TestCase] = map(key)
      val failures = testCases.filter(tc => tc.failureDetails.nonEmpty)
      val t = testCases.head
      FlakyTest(t.clazz, t.test, testCases.length, failures)
    }.toList
  }

  def createReport(projectName: String,
                   timeDetails: TimeDetails,
                   iterationNames: Seq[String],
                   flakyDir: File = new File("target/flaky-report")): FlakyTestReport = {
    val testRunDirs = flakyDir.listFiles
      .filter(_.isDirectory)
      .filter(f => iterationNames.contains(f.getName))
      .toList
    val testRuns = testRunDirs.map { dir =>
      val testCases = processFolder(dir)
      TestRun(s"${dir.getName}", testCases)
    }
    val flakyTests = findFlakyTests(testRuns)
    FlakyTestReport(projectName, timeDetails, testRuns, flakyTests)
  }

  def isFailed(dir: File): Boolean = {
    if (dir.exists()) {
      val testCases: List[TestCase] = processFolder(dir)
      testCases.exists(tc => tc.failureDetails.nonEmpty)
    } else {
      false
    }
  }
}
