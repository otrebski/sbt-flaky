organization := "pl.otrebski"

name := "sbt-flaky"

version := "0.3-SNAPSHOT"

//scalaVersion := "2.11.8"

sbtPlugin := true

javaVersionPrefix in javaVersionCheck := Some("1.8") //Should I compile with specific version?

ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-XX:MaxPermSize=256M", "-Dplugin.version=" + version.value)
}

scriptedBufferLog := false

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

publishMavenStyle := false

bintrayOrganization := Some("otrebski")

bintrayPackageLabels := Seq("sbt", "flaky-test")

bintrayRepository := "otrebski"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

