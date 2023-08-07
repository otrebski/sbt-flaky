package u
import collection.mutable.Stack
import org.scalatest._
import flatspec._
import matchers._

class USpec extends AnyFlatSpec with should.Matchers {

  "A U class" should "returns value" in {
    val u = U(1)

    u.v should be(1)
  }

}
