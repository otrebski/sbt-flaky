package flaky

import sbt.util.Level

class DummySbtLogger extends sbt.Logger {
  override def trace(t: => Throwable): Unit = {}

  override def success(message: => String): Unit = {}

  override def log(level: Level.Value, message: => String): Unit = {}
}
