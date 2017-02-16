
import sbt._

object FlakyCommand {

  val testReports = new java.io.File("./target/test-reports")
  val dir = new java.io.File("./target/flaky-report")
  val taskKeys = List(Keys.test in Test)
  val logFiles = List("./target/test.log", "./target/test.log.xml")

  //TODO run testOnly instead of test
  def flaky: Command = Command("flaky")(parser) { (state, args) =>

    println(s"Executed flaky command")
    case class TimeReport(times: Int, duration: Long) {
      def estimate(timesLeft: Int): String = {
        val r = (duration / times) * timesLeft
        s"${r / 1000}s"
      }

      def estimateCountIn(timeLeft: Long): String = {
        val r = timeLeft / (duration / times)
        s"$r times"
      }
    }
    dir.mkdirs()

    val start = System.currentTimeMillis

    args match {
      case Times(count) =>
        for (i <- 1 to count) {
          println(s"Running tests: $i")
          taskKeys.foreach(taskKey => Project.runTask(taskKey, state))
          moveFiles(i, logFiles)
          val timeReport = TimeReport(i, System.currentTimeMillis - start)
          println(s"Test iteration $i finished. ETA: ${timeReport.estimate(count - i)}")
        }
      case Duration(minutes) =>
        var i = 1
        val end = start + minutes.toLong * 60 * 1000
        while (System.currentTimeMillis < end) {
          println(s"Running tests: $i")
          taskKeys.foreach(taskKey => Project.runTask(taskKey, state))
          moveFiles(i, logFiles)
          val timeReport = TimeReport(i, System.currentTimeMillis - start)
          val timeLeft = end - System.currentTimeMillis
          println(s"Test iteration $i finished. ETA: ${timeLeft / 1000}s [${timeReport.estimateCountIn(timeLeft)}]")
          i = i + 1
        }
      case FirstFailure =>
        var i = 1
        var foundFail = false
        while (!foundFail) {
          println(s"Running tests: $i")

          taskKeys.foreach(taskKey => Project.runTask(taskKey, state))
          if (Flaky.isFailed(testReports)) {
            foundFail = true
          }
          moveFiles(i, logFiles)
          i = i + 1
        }

    }
    val report = Flaky.createReport()
    val slackMsg = Slack.render(report)
    //    println(slackMsg)
    val hookId = for (u <- Option(System.getProperty("SLACK_HOOKID"))) yield u
    hookId.foreach(h => Slack.send(h, slackMsg))

    state
  }

  private def parser(state: State) = {
    import sbt.complete.DefaultParsers._
    val times = (Space ~> "times=" ~> NatBasic)
      .examples("times=5", "times=25", "times=100")
      .map { a => Times(a) }
    val duration = (Space ~> "duration=" ~> NatBasic)
      .examples("duration=15", "duration=60")
      .map { a => Duration(a.toLong) }

    val firstFailure = (Space ~> "firstFail")
      .examples("firstFail")
      .map { _ => FirstFailure }

    times | duration | firstFailure
  }

  private def moveFiles(iteration: Int, logFiles: List[String]): Unit = {
    val iterationDir = new java.io.File(dir, s"$iteration")
    testReports.renameTo(iterationDir)
    logFiles.foreach(f => new File(f).renameTo(new File(iterationDir, new File(f).getName)))
  }
}

sealed trait FlakyArgs

case class Times(count: Int) extends FlakyArgs

case class Duration(duration: Long) extends FlakyArgs

case object FirstFailure extends FlakyArgs
