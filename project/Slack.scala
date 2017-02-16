object Slack {

  def render(flaky: List[FlakyTest]):String = {
    val flakyText = flaky
      .filter(_.failures > 0)
      .sortBy(_.failures)
      .reverse
      .map(flaky => s"${flaky.clazz}: ${flaky.test} ${flaky.failures*100/flaky.totalRun}%")
      //TODO group by class
      .mkString("\\n")

       val msg = s"""
   {
       "attachments": [
       {
           "fallback": "Flaky test result",
           "color": "danger",
           "pretext": "Flaky test report",
           "author_name": "sbt-flaky",
           "title": "Flaky test result",
           "text": "$flakyText",
           "footer": "sbt-flaky",
           "ts": ${System.currentTimeMillis}
       }
       ]
   }
   """
   msg
  }
}
