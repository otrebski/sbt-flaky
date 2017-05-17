package flaky.history

import java.io.{File, FileFilter, InputStream}
import java.text.SimpleDateFormat
import java.util.Date

import flaky.{Flaky, FlakyTestReport, Io}
import org.apache.commons.vfs2.VFS

import scala.util.Try
import scala.xml.XML

class History(project: String, historyDir: File, flakyReportDir: File, projectDir: File) {

  private val zipFileFilter = new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.endsWith(".zip")
  }

  private def runFiles(historyDir: File): List[File] = historyDir.listFiles(zipFileFilter).toList

  def addCurrentToHistory(): Unit = {
    val timestamp = System.currentTimeMillis()

    val date = new SimpleDateFormat(History.dateFormat).format(new Date(timestamp))
    val gitCommit = Try {
      Git(projectDir).currentId()
    }.toOption
    val historyReportDescription = HistoryReportDescription(timestamp, gitCommit)
    HistoryReportDescription.save(historyReportDescription, new File(flakyReportDir, History.descriptorFile))
    Zip.compressFolder(new File(historyDir, s"$date.zip"), flakyReportDir)
  }

  def removeToOldFromHistory(maxToKeep: Int): Unit = {
    runFiles(historyDir)
      .take(Math.max(runFiles(historyDir).size - maxToKeep, 0))
      .foreach(_.delete())
  }

  def createHistoryReport(): HistoryReport = {

    val historicalRuns: List[HistoricalRun] = runFiles(historyDir)
      .map(History.loadHistory)
    val date = new SimpleDateFormat("HH:mm dd-MM-YYYY").format(new Date())
    HistoryReport(project, date, historicalRuns)
  }


  def processHistory(): HistoryReport = {
    historyDir.mkdirs()
    addCurrentToHistory()
    removeToOldFromHistory(20)
    createHistoryReport()
  }
}


case class HistoryReportDescription(timestamp: Long, gitCommitHash: Option[String])

object HistoryReportDescription {

  def load(in: InputStream): HistoryReportDescription = {
    val descriptorXml = XML.load(in)
    val timestamp = (descriptorXml \ "timestamp").text.trim.toLong
    val gitHash = (descriptorXml \ "gitCommitHash").text.trim
    HistoryReportDescription(timestamp, Some(gitHash))
  }

  def save(historyReportDescription: HistoryReportDescription, file: File): Unit = {
    val xml =
      <HistoryReportDescription>
        <timestamp>
          {historyReportDescription.timestamp}
        </timestamp>
        <gitCommitHash>
          {historyReportDescription.gitCommitHash.getOrElse("")}
        </gitCommitHash>
      </HistoryReportDescription>
    val prettyXml = new scala.xml.PrettyPrinter(80, 2).format(xml)
    Io.writeToFile(file, prettyXml)
  }
}

object History {
  val descriptorFile = "descriptor.xml"
  val dateFormat = "yyyyMMdd-HHmmss"

  def loadHistory: (File) => HistoricalRun = {
    file => {
      val manager = VFS.getManager
      val uri = file.toURI.toString.replace("file:/", "zip:/")
      val fo = manager.resolveFile(uri)
      val report: FlakyTestReport = Flaky.createReportFromHistory(fo)
      val descriptorFile = Option(fo.getChild(History.descriptorFile))
      val dateFromFileName = file.getName.replace(".zip","")
      val hrd = descriptorFile
        .filter(_.exists())
        .map(f => HistoryReportDescription.load(f.getContent.getInputStream))
        .getOrElse(HistoryReportDescription(new SimpleDateFormat(dateFormat).parse(dateFromFileName).getTime, None))
      HistoricalRun(hrd, report)
    }
  }
}
