/*
* Common code for twitter clients
*/
import akka.actor.ActorSystem
import akka.actor.Props
import com.typesafe.config.ConfigFactory

object Client {

  def main(args: Array[String]) {
  
    val system = ActorSystem("TwitterClient")
    if (args.length > 5) {
    
      println("Starting client with ClientID = " + args(2))
      var totalUsers: Int = args(0).toInt
      var numClient: Int = args(1).toInt
      var clientID: Int = args(2).toInt
      var ServerIP:String = args(3)
      var ServerPort:String = args(4)
      var runTime:Int = args(5).toInt
      
      val ClientCoorinatorService = system.actorOf(Props(new ClientCoordinator(totalUsers, numClient, clientID, ServerIP, ServerPort, system, runTime)),name = "ClientCoorinator")
            

    } else {
      println("Usage : Client <Number of Users> <Number of Client Machines> <ClientID> <Server IP> <Server Port> <run time of client>")
    }
  }
}