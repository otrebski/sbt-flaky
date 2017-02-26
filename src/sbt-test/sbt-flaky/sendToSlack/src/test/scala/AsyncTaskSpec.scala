import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.Random


class AsyncTaskSpec extends FlatSpec with Matchers with LazyLogging {


  it should "complete async task" in {
    implicit val ec = ExecutionContext.global
    val asyncTask = Future {
      Thread.sleep(40 + Random.nextInt(20))
      "Result"
    }
    Await.result(asyncTask, 55 millis) shouldBe "Result"
  }
}
