import com.typesafe.scalalogging._
import org.scalatest._

import scala.collection.mutable

class ExampleSpec extends FlatSpec with Matchers with LazyLogging {

  it should "fail sometimes" in {
    (System.currentTimeMillis % 10 < 8) should be(true)
  }

  it should "fail randomly often" in {
    new java.util.Random().nextInt(2) should not be 0
  }

  it should "fail randomly" in {
    new java.util.Random().nextInt(4) should not be 0
  }

  it should "fail randomly sometimes" in {
    new java.util.Random().nextInt(8) should not be 0
  }
}
