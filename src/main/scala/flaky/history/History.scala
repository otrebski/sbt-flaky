package flaky.history

import java.io.{File, FileFilter}
import java.text.SimpleDateFormat
import java.util.Date

import flaky.{Flaky, FlakyTestReport}
import org.apache.commons.vfs2.VFS

class History(project:String, historyDir: File, flakyReportDir: File) {

  private val zipFileFilter = new FileFilter {
    override def accept(pathname: File): Boolean = pathname.getName.endsWith(".zip")
  }

  private def runFiles(historyDir: File): List[File] = historyDir.listFiles(zipFileFilter).toList

  def addCurrentToHistory(): Unit = {
    val date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date())
    Zip.compressFolder(new File(historyDir, s"$date.zip"), flakyReportDir)
  }

  def removeToOldFromHistory(maxToKeep: Int): Unit = {
    runFiles(historyDir)
      .take(Math.max(runFiles(historyDir).size - maxToKeep, 0))
      .foreach(_.delete())
  }


  def createHistoryReport(): HistoryReport = {
    val manager = VFS.getManager
    val historicalRuns: List[HistoricalRun] = runFiles(historyDir)
      .map(file => {
        val uri = file.toURI.toString.replace("file:/", "zip:/")
        val fo = manager.resolveFile(uri)
        val report: FlakyTestReport = Flaky.createReportFromHistory(fo)
        HistoricalRun(file.getName.replace(".zip", ""), report)
      })
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
