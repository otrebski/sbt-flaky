package flaky

import java.io.File

trait Unzip {

  def unzip(zipped: File, unzipDir: File, deleteOnExit: Boolean = true): Unit = {
    import java.io.{FileInputStream, FileOutputStream}
    import java.util.zip.ZipInputStream
    val fis = new FileInputStream(zipped)
    val zis = new ZipInputStream(fis)

    unzipDir.mkdirs()
    Stream
      .continually(zis.getNextEntry)
      .takeWhile(_ != null)
      .foreach { file =>
        if (file.isDirectory) {
          val dir = new File(unzipDir, file.getName)
          dir.mkdirs()
          if (deleteOnExit){
            dir.deleteOnExit()
          }
        } else {
          val file1 = new File(unzipDir, file.getName)
          if (deleteOnExit){
            file1.deleteOnExit()
          }
          val fout = new FileOutputStream(file1)
          val buffer = new Array[Byte](1024)
          Stream.continually(zis.read(buffer)).takeWhile(_ != -1).foreach(fout.write(buffer, 0, _))
        }
      }
  }
}
