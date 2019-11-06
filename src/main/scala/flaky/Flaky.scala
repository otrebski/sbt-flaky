package flaky

import java.io.File

import org.apache.commons.vfs2.FileObject

import scala.collection.immutable.{Iterable, Seq}
import scala.language.postfixOps
import scala.util.Try
import scala.xml.{Elem, NodeSeq, XML}

case class Test(clazz: String, test: String) {
  def classNameOnly(): String = clazz.split('.').lastOption.getOrElse("<?>")
}

case class TestCase(runName: String,
                    test: Test,
                    time: Float = 0f,
                    failureDetails: Option[FailureDetails] = None
                   )

case class FailureDetails(message: String, ftype: String, stacktrace: String) {
  def withoutStacktraceMessage(): FailureDetails = {
    val newStacktraceWithoutMessage = stacktrace.substring(Math.max(stacktrace.indexOf("\n"), 0))
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

case class FlakyTest(test: Test,
                     totalRun: Int,
                     failedRuns: List[TestCase]
                    ) {

  def failures(): Int = failedRuns.size

  def failurePercent(): Float = (100f * failures()) / totalRun

  def groupByStacktrace(): List[List[TestCase]] = {
    failedRuns.map { tc =>
      tc.copy(failureDetails = tc.failureDetails.map(_.withoutStacktraceMessage()))
    }.groupBy(_.failureDetails.map(_.stacktrace))
      .values.toList
  }
}

case class TimeDetails(start: Long, end: Long) {
  def duration(): Long = end - start
}

case class FlakyCase(test: Test, runNames: List[String], message: Option[String], stacktrace: String, allMessages: Set[String])


case class FlakyTestReport(projectName: String, timeDetails: TimeDetails, testRuns: List[TestRun], flakyTests: List[FlakyTest]) {
  def groupFlakyCases(): Map[String, List[FlakyCase]] = {
    flakyTests
      .filter(_.failures > 0)
      .groupBy(t => t.test.clazz)
      .map { kv =>
        val clazzTestName = kv._1
        val list: Seq[FlakyTest] = kv._2

        val text: Iterable[List[FlakyCase]] = list
          .groupBy(_.test)
          .flatMap {
            case (test, listOfFlakyTests) =>
              listOfFlakyTests.map {
                _.groupByStacktrace()
                  .map { list =>
                    val runNames: List[String] = list.map(_.runName).sorted
                    val messages: Seq[String] = list.flatMap(_.failureDetails).map(_.message)
                    val msg: Option[String] = findCommonString(messages.toList)
                    val stacktrace = list.headOption.flatMap(_.failureDetails.flatMap(_.firstNonAssertStacktrace())).getOrElse("")
                    FlakyCase(test, runNames, msg, stacktrace, messages.toSet)
                  }.toList
              }
          }

        (clazzTestName, text.flatten.toList)
      }
  }

  def successProbabilityPercent(): Float = {
    val totalRuns = testRuns.size
    val successProbability = flakyTests
      .filter(_.test.test != "(It is not a test)")
      // Issue #38 take int account that some test are failing as test "(It is not a test)"
      .map(t => (t.failedRuns.length + totalRuns - t.totalRun).toFloat / totalRuns)
      .foldLeft(1f)((acc, x) => acc * (1 - x))
    100 * successProbability
  }

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

      val failureDetails: Option[FailureDetails] = fail.headOption
        .orElse(error.headOption)
        .map { head =>
          FailureDetails(
            head \ "@message" text,
            head \ "@type" text,
            head.text)
        }

      val test = Test(className.text, name.text)
      TestCase(
        runName,
        test,
        time.text.toFloat,
        failureDetails
      )
    }.toList
  }

  def processFolder(dir: File): List[TestCase] = {
    dir.listFiles.toList
      .map(_.getAbsolutePath)
      .filter(_.endsWith("xml"))
      .map(x => Try {
        XML.loadFile(x)
      })
      .filter(_.isSuccess)
      .map(_.get)
      .flatMap { xml => parseJunitXmlReport(dir.getName, xml) }
  }

  def processFolder(dir: FileObject): List[TestCase] = {
    dir.getChildren
      .filter(_.getName.getBaseName.endsWith(".xml"))
      .map(x => Try {
        XML.load(x.getContent.getInputStream)
      })
      .filter(_.isSuccess)
      .map(_.get)
      .flatMap { xml => parseJunitXmlReport(dir.getName.getBaseName, xml) }.toList
  }

  def findFlakyTests(list: List[TestRun]): List[FlakyTest] = {
    val map = list.flatMap(tr => tr.testCases)
      .groupBy(tc => tc.test)

    map.keySet.map { key =>
      val testCases: List[TestCase] = map(key)
      val failures = testCases.filter(tc => tc.failureDetails.nonEmpty)
      val t = testCases.head.test
      FlakyTest(t, testCases.length, failures)
    }.toList
  }

  def createReport(projectName: String,
                   timeDetails: TimeDetails,
                   iterationNames: List[String],
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

  def createReportFromHistory(zippedFolder: FileObject): FlakyTestReport = {
    val testRunDirs = zippedFolder
      .getChildren
      .filter(_.isFolder)
      .toList
    val testRuns = testRunDirs.map { dir =>
      val testCases = processFolder(dir)
      TestRun(s"${dir.getName}", testCases)
    }
    val flakyTests = findFlakyTests(testRuns)
    FlakyTestReport("", TimeDetails(0, 0), testRuns, flakyTests)
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
