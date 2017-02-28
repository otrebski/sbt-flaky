package flaky


import sbt._

object FlakyCommand {

  val testReports = new java.io.File("./target/test-reports")
  val dir = new java.io.File("./target/flaky-report")
  val taskKeys = List(Keys.test in Test)
  val logFiles = List("./target/test.log", "./target/test.log.xml")

  //TODO run testOnly instead of test
  def flaky: Command = Command("flaky")(parser) { (state, args) =>
    state.log.info(s"Executing flaky command")
    val slackHook: Option[String] = Project.extract(state).get(FlakyPlugin.autoImport.flakySlackHook)

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
          state.log.info(s"Running tests: $i")
          taskKeys.foreach(taskKey => Project.runTask(taskKey, state))
          moveFiles(i, logFiles)
          val timeReport = TimeReport(i, System.currentTimeMillis - start)
          state.log.info(s"Test iteration $i finished. ETA: ${timeReport.estimate(count - i)}")
        }
      case Duration(minutes) =>
        var i = 1
        val end = start + minutes.toLong * 60 * 1000
        while (System.currentTimeMillis < end) {
          state.log.info(s"Running tests: $i")
          taskKeys.foreach(taskKey => Project.runTask(taskKey, state))
          moveFiles(i, logFiles)
          val timeReport = TimeReport(i, System.currentTimeMillis - start)
          val timeLeft = end - System.currentTimeMillis
          state.log.info(s"Test iteration $i finished. ETA: ${timeLeft / 1000}s [${timeReport.estimateCountIn(timeLeft)}]")
          i = i + 1
        }
      case FirstFailure =>
        var i = 1
        var foundFail = false
        while (!foundFail) {
          state.log.info(s"Running tests: $i")

          taskKeys.foreach(taskKey => Project.runTask(taskKey, state))
          if (Flaky.isFailed(testReports)) {
            foundFail = true
          }
          moveFiles(i, logFiles)
          i = i + 1
        }

    }
    //TODO use dir from task config
    val report = Flaky.createReport()
    val name = Project.extract(state).get(sbt.Keys.name)
    state.log.info(TextReport.render(name, report))
    slackHook.foreach { hookId =>
      val slackMsg = Slack.render(name, report)
      Slack.send(hookId, slackMsg, state.log)
    }

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
    if (iterationDir.exists()) {
      iterationDir.delete()
    }
    testReports.renameTo(iterationDir)
    logFiles.foreach(f => new File(f).renameTo(new File(iterationDir, new File(f).getName)))
  }
}

sealed trait FlakyArgs

case class Times(count: Int) extends FlakyArgs

case class Duration(duration: Long) extends FlakyArgs

case object FirstFailure extends FlakyArgs
