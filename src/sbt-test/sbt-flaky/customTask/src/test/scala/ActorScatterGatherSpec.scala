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
class ActorScatterGatherSpec
  extends TestKit(ActorSystem("ActorScatterGather", ConfigFactory.parseString(ActorScatterGatherSpec.config)))
    with DefaultTimeout
    with ImplicitSender
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with LazyLogging {

  import ActorScatterGatherSpec._

  override def afterAll {
    shutdown()
  }


  "A Scatter-Gather path" should {

    "Forward one element list" in {
      val scatter = system.actorOf(Props(classOf[Scatter], testActor))
      within(150 millis) {
        scatter ! Protocol.ListOfStrings(List("test1"))
        expectMsg(Protocol.Joined(List("test1")))
      }
    }

    "Forward 2 element list" in {
      val scatter = system.actorOf(Props(classOf[Scatter], testActor))
      within(150 millis) {
        val list = (1 to 2).map(i => s"message $i").toList
        scatter ! Protocol.ListOfStrings(list)
        expectMsg(Protocol.Joined(list))
      }
    }
    "Forward 4  element list" in {
      val scatter = system.actorOf(Props(classOf[Scatter], testActor))
      within(150 millis) {
        val list = (1 to 4).map(i => s"message $i").toList
        scatter ! Protocol.ListOfStrings(list)
        expectMsg(Protocol.Joined(list))
      }
    }

  }
}

object ActorScatterGatherSpec {
  // Define your test specific configuration here
  val config =
    """
    akka {
      loglevel = "WARNING"
    }
    """

  /**
    * An Actor that scatter message
    */
  class Scatter(next: ActorRef) extends Actor with LazyLogging {
    def receive = {
      case Protocol.ListOfStrings(list) =>
        logger.info(s"Scattering list of strings")
        val gather = context.system.actorOf(Props(classOf[Gather], next, list.size))
        list.foreach {
          string =>
            Thread.sleep(10)
            logger.info(s"Creating processor actor for $string and forward message")
            context.system.actorOf(Props(classOf[Processor], gather)) ! Protocol.OneString(string)
        }
    }
  }

  /**
    * An Actor that scatter message
    */
  class Processor(next: ActorRef) extends Actor with LazyLogging {

    import akka.actor._

    def receive = {
      case Protocol.OneString(msg) =>
        logger.info(s"Received OneString($msg)")
        import scala.concurrent.ExecutionContext.Implicits.global
        import scala.concurrent.duration._
        context.system.scheduler.scheduleOnce(10 millis, self, Protocol.DoneString(msg))
      case Protocol.DoneString(msg) =>
        logger.info(s"Processing of $msg is done")
        next ! Protocol.OneString(msg)
        self ! PoisonPill
    }
  }


  /**
    * An Actor that forwards every message to a next Actor
    */
  class Gather(next: ActorRef, count: Int) extends Actor with LazyLogging {
    var list: List[String] = List.empty[String]

    def receive: PartialFunction[Any, Unit] = {
      case Protocol.OneString(msg) =>
        logger.info(s"Received OneString($msg)")
        list = msg :: list
        logger.info(s"Current size is ${list.size}")
        if (count == list.size) {
          next ! Protocol.Joined(list.reverse)
        }
    }
  }

  case object Protocol {

    case class ListOfStrings(list: List[String])

    case class OneString(msg: String)

    case class DoneString(msg: String)

    case class Joined(list: List[String])

  }

}
