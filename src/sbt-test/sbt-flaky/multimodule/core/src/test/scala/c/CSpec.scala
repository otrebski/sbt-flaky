package c
import collection.mutable.Stack
import org.scalatest._
import flatspec._
import matchers._

class CSpec extends AnyFlatSpec with should.Matchers {

  "A C class" should "returns value" in {
    val c = C(1)

    c.v should be(1)
  }

}
