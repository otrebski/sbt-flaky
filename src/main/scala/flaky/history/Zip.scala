package flaky.history

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.{ZipEntry, ZipOutputStream}

object Zip {


  def compressFolder(zipFilePath: File, folder: File): Unit = {
    import java.io.File
    def recursiveListFiles(f: File): Array[File] = {
      val these = f.listFiles
      these.filter(_.isFile) ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
    }

    val toList = recursiveListFiles(folder).toList
    compress(zipFilePath, folder, toList)
  }

  def compress(zipFilePath: File, root: File, files: List[File]): Unit = {
    val zip = new ZipOutputStream(new FileOutputStream(zipFilePath))
    val rootPath = root.getAbsolutePath
    try {
      for (file <- files) {
        zip.putNextEntry(new ZipEntry(file.getAbsolutePath.substring(rootPath.length)))
        val in = new FileInputStream(file)
        try {
          Iterator
            .continually(in.read())
            .takeWhile(_ > -1)
            .foreach(zip.write)
        } finally {
          in.close()
        }
        zip.closeEntry()
      }
    }
    finally {
      zip.close()
    }
  }
}
