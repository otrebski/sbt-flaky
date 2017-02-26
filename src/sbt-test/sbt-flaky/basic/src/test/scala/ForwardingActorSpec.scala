import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{DefaultTimeout, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps


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
    "Forwards in chain of 100 actors" in {
      val forwardRef = system.actorOf(Props(classOf[ForwardingActor], testActor))
      val entryRef = (1 to 100).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      within(3 seconds) {
        val messages = (1 to 10).map(i => s"Test msg $i")
        messages.foreach(entryRef ! _)
        messages.foreach(msg => expectMsg(msg))
      }
    }

    "Forwards in a chains of 100 and 105" in {
      val forwardRef = system.actorOf(Props(classOf[ForwardingActor], testActor))
      val entryRef1 = (1 to 100).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      val entryRef2 = (1 to 105).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      within(500 millis) {
        entryRef1 ! "test1"
        entryRef2 ! "test2"
        expectMsg("test1")
        expectMsg("test2")
      }
    }

    "Forwards in a chains of 100 and 100 with expectMsgAllOf" in {
      val forwardRef = system.actorOf(Props(classOf[ForwardingActor], testActor))
      val entryRef1 = (1 to 100).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      val entryRef2 = (1 to 100).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      within(500 millis) {
        entryRef1 ! "test1"
        entryRef2 ! "test2"
        entryRef2 ! "test3"
        expectMsgAllOf("test1", "test2", "test3")
      }
    }

    "Forwards in a chains of 100 and 110" in {
      val forwardRef = system.actorOf(Props(classOf[ForwardingActor], testActor))
      val entryRef1 = (1 to 100).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      val entryRef2 = (1 to 110).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      within(500 millis) {
        entryRef1 ! "test1"
        entryRef2 ! "test2"
        expectMsg("test1")
        expectMsg("test2")
      }
    }
    "Forwards in a chains of 100 and 120" in {
      val forwardRef = system.actorOf(Props(classOf[ForwardingActor], testActor))
      val entryRef1 = (1 to 100).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
      val entryRef2 = (1 to 120).foldLeft(forwardRef)((ref, i) => system.actorOf(Props(classOf[ForwardingActor], ref)))
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
  val config =
    """
    akka {
      loglevel = "WARNING"
    }
    """

  /**
    * An Actor that forwards every message to a next Actor
    */
  class ForwardingActor(next: ActorRef) extends Actor with LazyLogging {
    def receive = {
      case msg =>
        logger.info(s"Forwarding message $msg to $next")
        next ! msg
    }
  }

}
