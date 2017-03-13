package flaky

object TextReport {

  def render(report:FlakyTestReport): String = {
    val projectName = report.projectName
    val flaky = report.flakyTests
    import scala.Console.{GREEN, RED, RESET, CYAN, BOLD}
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
    flakyTesRuns.foreach { flaky =>
      sb.append(s"$RED${flaky.clazz}: ${flaky.test} failed in runs: ${flaky.failedRuns.map(_.runName).mkString(", ")}\n")
    }
    if (flakyTesRuns.isEmpty) {
      sb.append(s"${GREEN}No flaky test detected")
    }
    sb.append(RESET)
    sb.toString
  }


}