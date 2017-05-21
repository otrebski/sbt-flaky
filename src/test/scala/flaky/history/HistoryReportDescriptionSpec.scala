package flaky.history

import java.io.{ByteArrayInputStream, File}

import org.scalatest.{Matchers, WordSpec}

import scala.io.Source

class HistoryReportDescriptionSpec extends WordSpec with Matchers {

  private val fileContent =
    """|<HistoryReportDescription>
       |  <timestamp> 123456 </timestamp>
       |  <gitCommitHash> abcdefg </gitCommitHash>
       |</HistoryReportDescription>""".stripMargin

  "HistoryReportDescription" should {
    "save to xml" in {
      val description = HistoryReportDescription(123456L, Some("abcdefg"))
      val tempFile = File.createTempFile("HistoryReportDescription", "suffix")
      HistoryReportDescription.save(description, tempFile)

      val string = Source.fromFile(tempFile).toList.mkString

      string shouldBe fileContent
    }

    "load from xml" in {
      val input = new ByteArrayInputStream(fileContent.getBytes)
      val hrd = HistoryReportDescription.load(input)

      hrd shouldBe HistoryReportDescription(123456L, Some("abcdefg"))

    }
  }


}
