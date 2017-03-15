package flaky

import org.scalatest.{Matchers, WordSpec}

class FailureDetailsSpec extends WordSpec with Matchers {


  "FailureDetails" should {

    "find first non test framework stacktrace line" in {
      val stacktrace =
        """org.junit.ComparisonFailure: expected:&lt;00:00:00.00[2]&gt; but was:&lt;00:00:00.00[0]&gt;
          |	at org.junit.Assert.assertEquals(Assert.java:115)
          |	at org.junit.Assert.assertEquals(Assert.java:144)
          |	at java.lang.Thread.getStackTrace(Thread.java:1552)
          |	at scala.lang.Thread.getStackTrace(Thread.java:1552)
          |	at tests.DateFormattingTest.formatParallelTest(DateFormattingTest.java:27)
          |	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
          |	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
          |	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
          |	at java.lang.reflect.Method.invoke(Method.java:483)
          |	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:50)
          |	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
          |	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:47)
          |	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)""".stripMargin

      val firstNonAssertStacktrace = FailureDetails("msg", "type", stacktrace).firstNonAssertStacktrace()

      firstNonAssertStacktrace shouldBe Some("	at tests.DateFormattingTest.formatParallelTest(DateFormattingTest.java:27)")
    }

    "remove message from stacktrace" in {
      val stacktrace =
        """org.junit.ComparisonFailure: expected:&lt;00:00:00.00[2]&gt; but was:&lt;00:00:00.00[0]&gt;
          |	at org.junit.Assert.assertEquals(Assert.java:115)
          |	at org.junit.Assert.assertEquals(Assert.java:144)
          |	at tests.DateFormattingTest.formatParallelTest(DateFormattingTest.java:27)
          |	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)""".stripMargin

      val expected =
        """
          |	at org.junit.Assert.assertEquals(Assert.java:115)
          |	at org.junit.Assert.assertEquals(Assert.java:144)
          |	at tests.DateFormattingTest.formatParallelTest(DateFormattingTest.java:27)
          |	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)""".stripMargin

      val w: FailureDetails = FailureDetails("msg", "type", stacktrace).withoutStacktraceMessage()

      w.stacktrace shouldBe expected
    }

  }
}
