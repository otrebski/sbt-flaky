package flaky

import java.io.{BufferedWriter, File, OutputStreamWriter, PrintWriter}
import java.net.{HttpURLConnection, URL}
import java.util.Scanner

import sbt.{File, Logger}

import scala.util.{Failure, Success, Try}

/**
  * Created by k.otrebski on 07/04/2017.
  */
object Io {

  def writeToFile(file:File, content:String ) = {
    new PrintWriter(file) {
      write(content)
      close()
    }
  }

  def sendToSlack(webHook: String, jsonMsg: String, log: Logger, backupFile: File): Unit = {
    log.info("Sending report to slack")
    log.debug("Dumping slack msg to file")
    new PrintWriter(backupFile) {
      write(jsonMsg)
      close()
    }

    val send: Try[Unit] = Try {
      val url = new URL(webHook)
      val urlConnection = url.openConnection().asInstanceOf[HttpURLConnection]
      // Indicate that we want to write to the HTTP request body
      urlConnection.setDoOutput(true)
      urlConnection.setRequestMethod("POST")

      // Writing the post data to the HTTP request body
      log.debug(jsonMsg)
      val httpRequestBodyWriter = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()))
      httpRequestBodyWriter.write(jsonMsg)
      httpRequestBodyWriter.close()

      val scanner = new Scanner(urlConnection.getInputStream())
      log.debug("Response from SLACK:")
      while (scanner.hasNextLine) {
        log.debug(s"Response from SLACK: ${scanner.nextLine()}")
      }
      scanner.close()
    }
    send match {
      case Success(_) => log.info("Notification successfully send to Slack")
      case Failure(e) => log.error(s"Can't send message to slack: ${e.getMessage}")
    }

  }
}
