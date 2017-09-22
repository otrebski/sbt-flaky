package subfolder
import com.typesafe.scalalogging._
import org.scalatest._

import scala.collection.mutable

class ExampleSpec extends FlatSpec with Matchers with LazyLogging {

  "A Stack" should "pop values in last-in-first-out order" in {
    logger.info("Staring test")
    val stack = new mutable.Stack[Int]
    stack.push(1)
    stack.push(2)
    stack.pop() should be(2)
    stack.pop() should be(1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new mutable.Stack[Int]
    a[NoSuchElementException] should be thrownBy {
      emptyStack.pop()
    }
  }

}
