import java.io.{File, FilenameFilter}

import flaky._
import flaky.history.{Git, History, HistoryReport}
import org.apache.commons.vfs2.VFS

object RenderHtml extends App with Unzip {
  println("Creating report")
  private val reportsDir = new File("target/flakyreports")
  private val dirWithReports = new File("src/test/resources/history8")

  val log = new DummySbtLogger()

  private val zipFile: File = dirWithReports
    .listFiles(new FilenameFilter {
      override def accept(dir: File, name: String): Boolean = name.endsWith("zip")
    }).minBy(_.getName)

  private val projectZip = new File("src/test/resources/gitrepo.zip")
  private val unzipDir = new File("target/unzipped/")
  println(s"Unzipping ${zipFile.getPath}")
  unzip(projectZip, unzipDir)
  private val projectDir = new File(unzipDir,"gitrepo")

  private val report = Flaky.createReportFromHistory(VFS.getManager.resolveFile(zipFile.toURI.toString.replace("file:/", "zip:/")))
  private val historyReport: HistoryReport = new History("My App", dirWithReports, new File(""), projectDir).createHistoryReport()

  FlakyCommand.createHtmlReports("My App", report, Some(historyReport), reportsDir, Git(projectDir), log)
  println(s"Reports created in ${reportsDir.getAbsolutePath}")
}
