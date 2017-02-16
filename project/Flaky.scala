import scala.collection.JavaConversions._
import scala.util.Try
import scala.util.Success
import java.io.File
import scala.xml.XML
import scala.xml.Elem

case class TestCase (
  clazz:String,
  test:String,
  time:Float,
  success:Boolean = true)

case class TestRun (
  name: String,
  testCases: List[TestCase]
)

case class FlakyTest (
  clazz: String,
  test: String,
  totalRun: Int,
  failures: Int,
  failedRuns: List[String]
)
object Flaky {

  private def toTestCases(xml: Elem): List[TestCase] = {
    val testCases = xml \\ "testcase"
    testCases.map { testcase =>
      val className = testcase \ "@classname"
      val name = testcase \ "@name"
      val time = testcase \ "@time"
      val fail = testcase \ "failure"
      val error = testcase \ "error"
      TestCase(
        className.text,
        name.text,
        time.text.toFloat,
        fail.isEmpty && error.isEmpty
      )
  }.toList
}

private def processFolder(dir: File) : List[TestCase] = {
  //TODO add xml filtering
  val f = dir.listFiles.toList;
  f
   .map(_.getAbsolutePath)
   .filter(_.endsWith("xml"))
   .map(x => Try{XML.loadFile(x)})
   .filter(_.isSuccess)
   .map(_.get)
   .flatMap { xml => toTestCases(xml)}
}

private def findFlakyTests(list: List[TestRun]): List[FlakyTest] = {
  case class Test(  clazz:String, test:String)
  val keys = list
    .flatMap(tr => tr.testCases)
    .map (tc => Test(tc.clazz, tc.test))
    .toSet

  val map = list.flatMap(tr => tr.testCases)
    .groupBy(tc => Test(tc.clazz, tc.test))

  map.keySet.map { key =>
    val testCases:List[TestCase] = map(key)
    val (successes, failures) = testCases.partition(tc => tc.success)
    val t = testCases.head
    val failedRuns = list
      .filter{_.testCases.exists{tc =>
         tc.clazz == t.clazz && tc.test == t.test && !tc.success
        }
      }.map(_.name)
    FlakyTest(t.clazz, t.test, testCases.length, failures.length, failedRuns)
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
    println ("\nHealthy tests:")
    flaky
      .filter(_.failures == 0)
      .foreach {healthy =>
        println(s"${healthy.test}")
      }
    println("\nFlaky tests:")
    val flakyTesRuns = flaky
      .filter(_.failures > 0)
      .sortBy(_.failures)
      .reverse
    flakyTesRuns
      .foreach {flaky =>
        println(s"${flaky.test} ${flaky.failures*100/flaky.totalRun}%")
    }
    println("\nDetails:")
    flakyTesRuns.foreach{ flaky =>
      println(s"${flaky.clazz}: ${flaky.test} failed in runs: ${flaky.failedRuns.mkString(", ")}")
    }
    flaky
  }

  def isFailed(dir:File): Boolean = {
    val testCases: List[TestCase] = processFolder(dir)
    !testCases.filter(tc => !tc.success).isEmpty
  }
}
