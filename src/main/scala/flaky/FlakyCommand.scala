package flaky

import flaky.FlakyPlugin._
import sbt._
import sbt.complete.Parser

object FlakyCommand {

  def flakyOnly: Command = Command("flakyOnly")(parserFlaky){
    (state, args) =>
      Project.extract(state).get(Keys.testOnly.)
      state
  }

  //TODO run testOnly instead of test
  def flaky: Command = Command("flaky")(parserFlaky) {
    (state, args) =>
      val targetDir = Project.extract(state).get(Keys.target)

      //TODO settingKey
      val testReports = new File(targetDir, "test-reports")

      val flakyReportsDir = new File(targetDir, Project.extract(state).get(autoImport.flakyReportsDir))
      val logFiles = Project.extract(state).get(autoImport.flakyAdditionalFiles)
      val logLevelInTask = Project.extract(state).get(autoImport.flakyLogLevelInTask)
      val slackHook: Option[String] = Project.extract(state).get(autoImport.flakySlackHook)
      val taskKeys: Seq[TaskKey[Unit]] = Project.extract(state).get(autoImport.flakyTask)
      val moveFilesF = moveFiles(flakyReportsDir, testReports, logFiles) _

      def runTasks(state: State, runIndex: Int) = {
        state.log.info(s"Running tests: $runIndex")
        taskKeys.foreach { taskKey =>
          val extracted = Project extract state
          import extracted._
          import sbt.Keys.logLevel
          val newState = append(Seq(logLevel in taskKey := logLevelInTask), state)
          Project.runTask(taskKey, newState, checkCycles = true)
        }
        moveFilesF(runIndex)
      }

      state.log.info(s"Executing flaky command")


      flakyReportsDir.mkdirs()

      val start = System.currentTimeMillis

      args match {
        case Times(count) =>
          for (i <- 1 to count) {
            runTasks(state, i)
            val timeReport = TimeReport(i, System.currentTimeMillis - start)
            state.log.info(s"${scala.Console.GREEN}Test iteration $i finished. ETA: ${timeReport.estimate(count - i)}${scala.Console.RESET}")
          }
        case Duration(minutes) =>
          var i = 1
          val end = start + minutes.toLong * 60 * 1000
          while (System.currentTimeMillis < end) {
            runTasks(state, i)
            val timeReport = TimeReport(i, System.currentTimeMillis - start)
            val timeLeft = end - System.currentTimeMillis
            val formattedSeconds = TimeReport.formatSeconds(timeLeft / 1000)
            state.log.info(s"${scala.Console.GREEN}Test iteration $i finished. ETA: ${formattedSeconds}s [${timeReport.estimateCountIn(timeLeft)}] ${scala.Console.RESET}")
            i = i + 1
          }
        case FirstFailure =>
          var i = 1
          var foundFail = false
          while (!foundFail) {
            runTasks(state, i)
            if (Flaky.isFailed(testReports)) {
              foundFail = true
            }
            i = i + 1
          }

      }
      val report = Flaky.createReport(flakyReportsDir)
      val name = Project.extract(state).get(sbt.Keys.name)
      state.log.info(TextReport.render(name, report))
      slackHook.foreach { hookId =>
        val slackMsg = Slack.render(name, report)
        Slack.send(hookId, slackMsg, state.log, flakyReportsDir)
      }
      state
  }

  private def parserFlaky(state: State) = {
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

  private def parserFlakyOnly(state: State) = {
    import sbt.complete.DefaultParsers._
    val times = (Space ~> "times=" ~> NatBasic )
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

  //https://github.com/sbt/sbt/blob/ef56db9885449d47f8ef200986bc9517b8cf2085/main/src/main/scala/sbt/Defaults.scala
  def testOnlyParser: (State, Seq[String]) => Parser[(Seq[String], Seq[String])] =
  { (state, tests) =>
    import sbt.complete.DefaultParsers._
    val selectTests = distinctParser(tests.toSet, true)
    val options = (token(Space) ~> token("--") ~> spaceDelimited("<option>")) ?? Nil
    selectTests ~ options
  }

  private def distinctParser(exs: Set[String], raw: Boolean): Parser[Seq[String]] =
  {
    import sbt.complete.DefaultParsers._, Parser.and
    val base = token(Space) ~> token(and(NotSpace, not("--", "Unexpected: ---")) examples exs)
    val recurse = base flatMap { ex =>
      val (matching, notMatching) = exs.partition(GlobFilter(ex).accept _)
      distinctParser(notMatching, raw) map { result => if (raw) ex +: result else matching.toSeq ++ result }
    }
    recurse ?? Nil
  }

  private def moveFiles(reportsDir: File, testReports: File, logFiles: List[File])(iteration: Int): Unit = {
    val iterationDir = new File(reportsDir, s"$iteration")
    if (iterationDir.exists()) {
      iterationDir.delete()
    }
    testReports.renameTo(iterationDir)
    logFiles.foreach(f => f.renameTo(new File(iterationDir, f.getName)))
  }
}

sealed trait FlakyArgs

case class Times(count: Int) extends FlakyArgs

case class Duration(duration: Long) extends FlakyArgs

case object FirstFailure extends FlakyArgs
