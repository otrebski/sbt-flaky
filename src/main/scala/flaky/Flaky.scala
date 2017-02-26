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

case class FailureDetails(message: String, ftype: String, stacktrace: String)

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
}

object Flaky {

  def parseJunitXmlReport(runName: String, xml: Elem): List[TestCase] = {
    val testCases = xml \\ "testcase"
    testCases.map { testcase =>
      val className = testcase \ "@classname"
      val name = testcase \ "@name"
      val time = testcase \ "@time"
      val fail: NodeSeq = testcase \ "failure"
      val error = testcase \ "error"
      //TODO extract data from error tag

      val failureDetails = fail.headOption.map { head =>
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

  def createReport(flakyDir: File = new File("target/flaky-report")): List[FlakyTest] = {
    val testRunDirs = flakyDir.listFiles.filter(_.isDirectory).toList
    val testRuns = testRunDirs.map { dir =>
      val testCases = processFolder(dir)
      TestRun(s"${dir.getName}", testCases)
    }
    findFlakyTests(testRuns)
  }

  def isFailed(dir: File): Boolean = {
    val testCases: List[TestCase] = processFolder(dir)
    testCases.exists(tc => tc.failureDetails.nonEmpty)
  }
}
