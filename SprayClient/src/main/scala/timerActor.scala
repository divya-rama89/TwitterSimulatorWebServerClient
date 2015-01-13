/*
 * timerActor.scala blocks for 5 minutes then notifies the remoteListener that time is up
 * timerActor.acala starts at the same time as remoteListener and runs on the server machine
 * 
 * @param actor-reference of remoteListener 
 */

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem

case object start //This is a message object of timerActor

class timerActor(actorsys: ActorSystem,runTime:Int) extends Actor{

  def receive={
    //this method blocks for 5 minutes
  	case start => 
      var startTime = System.currentTimeMillis()
      while(System.currentTimeMillis() < startTime + runTime*1000){
        
      } 
      sender ! "die"
      actorsys.shutdown
  }
}