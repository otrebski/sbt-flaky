package flaky.report

import flaky.FlakyTestReport

import scala.collection.immutable.Iterable

object TextReport {

  def render(report:FlakyTestReport): String = {
    val projectName = report.projectName
    val flaky = report.flakyTests
    import scala.Console._
    val sb = new StringBuilder
    sb.append(s"$CYAN Flaky tests result for $BOLD $projectName\n")
      .append(GREEN)
      .append("Healthy tests:\n")

    flaky
      .filter(_.failures == 0)
      .foreach { healthy =>
        sb.append(s"$GREEN${healthy.test}\n")
      }
    sb.append("\n")
      .append(RED)
      .append("Flaky tests:\n")

    val flakyTesRuns = flaky
      .filter(_.failures > 0)
      .sortBy(_.failures())
      .reverse
    flakyTesRuns
      .foreach { flaky =>
        sb.append(f"$RED${flaky.test} ${flaky.failures * 100f / flaky.totalRun}%.2f%%\n")
      }
    sb.append(s"\n${CYAN}Details:\n")

    val failedDetails: Iterable[String] = report.groupFlakyCases()
      .map {
        case (testClass, flakyTestCases) =>
          val flakyTestsDescription: String = flakyTestCases
            .sortBy(_.runNames.size)
            .map {
              fc =>
                val test = fc.test
                val message = fc.message.getOrElse("?")
                val runNames = fc.runNames.sorted.mkString(", ")
                val text =
                  s"""| [${fc.runNames.size} times] $RED$test$RESET
                      |   In following test runs: $runNames
                      |   Message: $RED$message$RESET
                      |    ${fc.stacktrace}
                      |    """.stripMargin
                text
            }.mkString("\n")
          s"""
             | $RED$testClass
             |$flakyTestsDescription$RESET
             |
           """.stripMargin
      }
    failedDetails.foreach(sb.append)
    if (flakyTesRuns.isEmpty) {
      sb.append(s"${GREEN}No flaky test detected")
    }
    sb.append(RESET)
    sb.toString
  }


}