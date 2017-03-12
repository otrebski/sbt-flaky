package flaky

case class TimeReport(times: Int, duration: Long) {
  def estimate(timesLeft: Int): String = {
    val seconds = ((duration / times) * timesLeft)/1000
    TimeReport.formatSeconds(seconds)
  }

  def estimateCountIn(timeLeft: Long): String = {
    val r = timeLeft / (duration / times)
    s"$r times"
  }
}

case object TimeReport {
  def formatSeconds(seconds:Long):String = {
    val d = java.time.Duration.ofSeconds(seconds)
    s"${d.toMinutes}m ${seconds % 60}s"
  }
}