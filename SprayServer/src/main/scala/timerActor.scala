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

class timerActor(actorsys: ActorSystem, aList : List[ActorRef],runTime : Int) extends Actor{

  def receive={
    //this method blocks for 5 minutes
  	case start => 
      var startTime = System.currentTimeMillis()
      while(System.currentTimeMillis() < startTime + runTime*1000){
        
      } 
      //send message to remoteListener that time is up and you need to stop the code 
      for (assigner <- aList){
        assigner ! "stop"
      }
      
      actorsys.shutdown
  }
}