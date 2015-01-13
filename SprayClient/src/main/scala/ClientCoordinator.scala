import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.PoisonPill
import scala.util.Random

class ClientCoordinator(totalUsers: Int, numClient: Int, clientID: Int, ServerIP: String, ServerPort: String, sys: ActorSystem, runTime:Int) extends Actor {

  var numUsers: Int = totalUsers / numClient
  var timer = sys.actorOf(Props(new timerActor(sys,runTime)), "timerActor")

  if (clientID + 1 == numClient) {
    for (i: Int <- (clientID * numUsers).toInt to totalUsers.toInt - 1) {
      context.actorOf(Props(new ClientWorker(i.toInt, totalUsers, ServerIP, ServerPort, sys, clientID)), name = i.toString)
    }
  } else {
    for (i: Int <- (clientID * numUsers).toInt to (numUsers * (clientID + 1) - 1).toInt) {
      context.actorOf(Props(new ClientWorker(i.toInt, totalUsers, ServerIP, ServerPort, sys, clientID)), name = i.toString)
    }
  }

  self ! "start"

  def receive = {
    case "start" =>
      //println("inside start of clientcoordinator")
      for (child <- context.children) {
        child ! "start"
      }
      timer ! "start"

    case "die" =>
      println("=" * 20)
      println("Shutting Down")
      println("=" * 20)
      for (child <- context.children) {
        child ! "cancelActions"
      }
      for (child <- context.children) {
        child ! PoisonPill
      }
      sys.shutdown()

    case _ => println("default case for Client Coordinator")
  }
} 