import java.io.File

import scala.util.Try
import scala.xml.{Elem, NodeSeq, XML}

case class TestCase( runName: String,
                     clazz: String,
                     test: String,
                     time: Float,
                     failureDetails:Option[FailureDetails] = None
                   )
case class FailureDetails(message:String, ftype:String, stacktrace :String)

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

  private def toTestCases(runName:String, xml: Elem): List[TestCase] = {
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

  private def processFolder(dir: File): List[TestCase] = {
    val f = dir.listFiles.toList
    f
      .map(_.getAbsolutePath)
      .filter(_.endsWith("xml"))
      .map(x => Try {
        XML.loadFile(x)
      })
      .filter(_.isSuccess)
      .map(_.get)
      .flatMap { xml => toTestCases(dir.getName, xml) }
  }

  private def findFlakyTests(list: List[TestRun]): List[FlakyTest] = {
    case class Test(clazz: String, test: String)
    val keys = list
      .flatMap(tr => tr.testCases)
      .map(tc => Test(tc.clazz, tc.test))
      .toSet

    val map = list.flatMap(tr => tr.testCases)
      .groupBy(tc => Test(tc.clazz, tc.test))

    map.keySet.map { key =>
      val testCases: List[TestCase] = map(key)
      val failures = testCases.filter(tc => tc.failureDetails.nonEmpty)
      val t = testCases.head
      FlakyTest(t.clazz, t.test, testCases.length, failures)
    }.toList
  }

  def createReport(): List[FlakyTest] = {
    //TODO use dir from task config
    val flakyDir = new File("target/flaky-report")
    val testRunDirs = flakyDir.listFiles.filter(_.isDirectory).toList
    val testRuns = testRunDirs.map { dir =>
      val testCases = processFolder(dir)
      TestRun(s"${dir.getName}", testCases)
    }
    val flaky = findFlakyTests(testRuns.toList)
    println("\nHealthy tests:")
    flaky
      .filter(_.failures == 0)
      .foreach { healthy =>
        println(s"${healthy.test}")
      }
    println("\nFlaky tests:")
    val flakyTesRuns = flaky
      .filter(_.failures > 0)
      .sortBy(_.failures())
      .reverse
    flakyTesRuns
      .foreach { flaky =>
        println(s"${flaky.test} ${flaky.failures * 100 / flaky.totalRun}%")
      }
    println("\nDetails:")
    flakyTesRuns.foreach { flaky =>
      println(s"${flaky.clazz}: ${flaky.test} failed in runs: ${flaky.failedRuns.mkString(", ")}")
    }
    flaky
  }

  def isFailed(dir: File): Boolean = {
    val testCases: List[TestCase] = processFolder(dir)
    testCases.exists(tc => tc.failureDetails.nonEmpty)
  }
}
