package flaky

import java.io.File

import flaky.history.{Git, History}
import org.scalatest.{Matchers, WordSpec}
import sbt.{FileFilter, Level}

class FlakyCommandTest extends WordSpec with Unzip with Matchers {

  private val zippedGitRepo = new File("./src/test/resources", "gitrepo.zip")
  private val unzippedGitDir = new File("target/")

  val log: _root_.sbt.Logger = new _root_.sbt.Logger() {
    override def trace(t: => Throwable): Unit = {}

    override def success(message: => String): Unit = {}

    override def log(level: Level.Value, message: => String): Unit = {}
  }

  "FlakyCommandTest" should {

    "createHtmlReports" in {
      //Goal of this test is also to generate report for visual check
      val reportDir = new File("./target/history8/20170523-231535")
      unzip(new File("./src/test/resources/history8/20170523-231535.zip"), reportDir)
      val dirs: Array[String] = reportDir.listFiles(new FileFilter {
        override def accept(pathname: File): Boolean = pathname.isDirectory
      }).map(_.getName)

      val htmlReportDir = new File("./target/example-report")
      val historyDir = new File("/Users/k.otrebski/tmp/history")

      val history = new History(
        project = "Project x",
        historyDir = historyDir,
        flakyReportDir = htmlReportDir,
        projectDir = new File("."))

      val historyReport1 = history.createHistoryReport()
      val timeDetails = TimeDetails(System.currentTimeMillis() - 9000000L, System.currentTimeMillis())
      val report = Flaky.createReport("Project X", timeDetails, dirs.toList, reportDir)

      unzip(zippedGitRepo, unzippedGitDir)
//      val gitDir = new File(unzippedGitDir, "gitrepo/")
      val gitDir = new File("/Users/k.otrebski/tmp/decant")
      val git = Git(gitDir)

      FlakyCommand.createHtmlReports("Project x", report, Some(historyReport1), htmlReportDir, git, log)

      new File(htmlReportDir,"index.html").exists shouldBe true
      new File(htmlReportDir,"flaky-report.html").exists shouldBe true
      new File(htmlReportDir,"flaky-report-history.html").exists shouldBe true
    }

  }
}
