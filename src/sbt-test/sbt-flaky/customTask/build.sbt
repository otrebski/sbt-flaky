name := "custom-task"

scalaVersion := "2.11.8"

val sampleTask1 = taskKey[Unit]("Create file sampleTask1.txt")
val sampleTask2 = taskKey[Unit]("Create file sampleTask2.txt")


lazy val library = (project in file("."))
  .settings(
    sampleTask1 := {println("creating file");new File("sampleTask1.txt").createNewFile()},
    sampleTask2 := {println("creating file");new File("sampleTask2.txt").createNewFile()}
  )
  .settings(flakyTask := Seq(sampleTask1, sampleTask2))
  .enablePlugins(FlakyPlugin)
