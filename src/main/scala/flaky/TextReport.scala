package flaky

object TextReport {
  def render(flaky: List[FlakyTest]): String = {
    val sb = new StringBuilder
    sb.append("\nHealthy tests:\n")
    flaky
      .filter(_.failures == 0)
      .foreach { healthy =>
        sb.append(s"${healthy.test}\n")
      }
    sb.append("\nFlaky tests:\n")
    val flakyTesRuns = flaky
      .filter(_.failures > 0)
      .sortBy(_.failures())
      .reverse
    flakyTesRuns
      .foreach { flaky =>
        sb.append(s"${flaky.test} ${flaky.failures * 100 / flaky.totalRun}%\n")
      }
    sb.append("\nDetails:\n")
    flakyTesRuns.foreach { flaky =>
      sb.append(s"${flaky.clazz}: ${flaky.test} failed in runs: ${flaky.failedRuns.map(_.runName).mkString(", ")}\n")
    }
    if (flakyTesRuns.isEmpty){
      sb.append("No flaky test detected")
    }
    sb.toString
  }
}