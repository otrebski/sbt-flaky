import scala.util.Random

import org.scalatest.BeforeAndAfterAll
import org.scalatest.WordSpecLike
import org.scalatest.Matchers

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging._

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.{ TestActors, DefaultTimeout, ImplicitSender, TestKit }
import scala.concurrent.duration._
import scala.collection.immutable


/**
 * a Test to show some TestKit examples
 */
class ForwardingActorSpec
  extends TestKit(ActorSystem("TestKitUsageSpec", ConfigFactory.parseString(ForwardingActorSpec.config)))
  with DefaultTimeout
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll
  with LazyLogging {
  import ForwardingActorSpec._

  val forwardRef = system.actorOf(Props(classOf[ForwardingActor], testActor))

  override def afterAll {
    shutdown()
  }


  "A ForwardingActor" should {

    "Forward a message it receives" in {
      within(500 millis) {
        forwardRef ! "test"
        expectMsg("test")
      }
    }
    "Forwards in a huge chain" in {
      val forwardRef = system.actorOf(Props(classOf[ForwardingActor], testActor))
      val entryRef = (1 to 100).foldLeft(forwardRef)((ref,i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      within(500 millis) {
        entryRef ! "test1"
        entryRef ! "test2"
        entryRef ! "test3"
        expectMsg("test1")
        expectMsg("test2")
        expectMsg("test3")
      }
    }
    "Forwards in a 2 huge chains" in {
      val forwardRef = system.actorOf(Props(classOf[ForwardingActor], testActor))
      val entryRef1 = (1 to 100).foldLeft(forwardRef)((ref,i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      val entryRef2 = (1 to 105).foldLeft(forwardRef)((ref,i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      within(500 millis) {
        entryRef1 ! "test1"
        entryRef2 ! "test2"
        expectMsg("test1")
        expectMsg("test2")
      }
    }

  }
}

object ForwardingActorSpec {
  // Define your test specific configuration here
  val config = """
    akka {
      loglevel = "WARNING"
    }
    """

  /**
   * An Actor that forwards every message to a next Actor
   */
  class ForwardingActor(next: ActorRef) extends Actor with LazyLogging{
    def receive = {
      case msg =>
        logger.info(s"Forwarding message $msg to $next")
        next ! msg
    }
  }

}
