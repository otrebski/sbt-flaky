organization := "pl.otrebski"

name := "sbt-flaky"

version := "0.1"

//scalaVersion := "2.11.8"

sbtPlugin := true

javaVersionPrefix in javaVersionCheck := Some("1.6") //TODO change this to 1.7 or 1.6 ?

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

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

resolvers += Resolver.typesafeRepo("releases")

