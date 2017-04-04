package flaky.history

import java.io.{File, FileFilter}
import java.text.SimpleDateFormat
import java.util.Date

import flaky.{Flaky, FlakyTestReport}
import org.apache.commons.vfs2.VFS


trait History {

  val historyDir: File
  val flakyReportDir: File

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

  def processHistory(): Unit = {
    val manager = VFS.getManager
    addCurrentToHistory()
    removeToOldFromHistory(20)
    val r: Seq[FlakyTestReport] = runFiles(historyDir)
      .map(file =>{
        val fo = manager.resolveFile(file.toURI)
         Flaky.createReportFromHistory(fo)
      }


  }

}
