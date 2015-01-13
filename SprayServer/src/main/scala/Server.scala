//import spray.routing.SimpleRoutingApp
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import org.json4s.Formats
import org.json4s.DefaultFormats
//import spray.routing.HttpService
import spray.httpx.Json4sSupport
//import akka.Main
import org.json4s.JsonAST.JObject
//import spray.util.LoggingContext
//import spray.routing.ExceptionHandler
//import spray.http.StatusCodes._
//import spray.routing._
//import scala.Some
//import spray.http._
//import spray.httpx.unmarshalling._
import spray.routing._

case class Tweet(uid: Int, text: String, mentions: Int, hashTag: String)
case class message(src : Int, dest : Int, msg:String)
object Server extends HttpService with Json4sSupport with SimpleRoutingApp {

  implicit def json4sFormats: Formats = DefaultFormats
  var assignerList: List[ActorRef] = List()
  implicit val timeout = Timeout(500.millis)
  implicit val system = ActorSystem("TwitterServer")
  val NUMBEROFSERVERASSIGNERS = 64
  val FOLLOWERSLIMIT = 10
  var SM: ActorRef = null
  var secondsCtr = System.currentTimeMillis()
  var tweetIDCtr: Int = 0
  var tweetIDCumulativeCtr = 0
  var requestCtr = 0

  def main(args: Array[String]) {

    var numUsers: Int = 0
    var numCli: Int = 0
    var myIP: String = "localhost"
    var myPort: Int = 0
    var runTime:Int = 0

    if (args.length > 4) {
      numUsers = (args(0).toInt)
      numCli = args(1).toInt
      myIP = args(2)
      myPort = args(3).toInt
      runTime = args(4).toInt
      for (x <- 0 to NUMBEROFSERVERASSIGNERS - 1) {
        var SA = system.actorOf(Props(new ServerAssigner(numUsers, system, x, NUMBEROFSERVERASSIGNERS, FOLLOWERSLIMIT)), x.toString)
        if (!assignerList.contains(SA)) {
          assignerList = assignerList :+ SA
        }
      }

      for (SA <- assignerList) {
        SA ! "init"
      }

      startServer(interface = myIP, port = myPort) {
        helloo ~ 
        requestRoute ~ 
        tweetRoute ~ 
        getFollowersListRoute ~ 
        requestMentionsRoute ~ 
        requestHashTagRoute ~
        getMyMsgs~
        message
      }

      var timer = system.actorOf(Props(new timerActor(system, assignerList,runTime)), "timerActor")
      timer ! "start"

      println("tweetIDctr\ttweetIDCumulativeCtr\trequestCtr")
      val tweetScheduler = system.scheduler.schedule(0 seconds, 1 seconds)(metricReset)

    } else {
      println("Usage : Server.scala <Number of Users> <Number of Clients> <IP number> <port number> <run time for server>")
    }

  }

  def metricReset = {
    println(tweetIDCtr + ",\t\t\t" + tweetIDCumulativeCtr + ",\t\t\t" + requestCtr)

    tweetIDCtr = 0
    requestCtr = 0
    secondsCtr += 1
  }
  
  lazy val requestRoute = {
    get {
      path("RequestTimeline" / IntNumber) { index =>
        {
          complete {
            requestCtr += 1
            (assignerList(index % NUMBEROFSERVERASSIGNERS) ? requestStatus(index)).mapTo[spray.json.JsArray]
              .map(s => s"$s")
              
          }
          //onFailure(myExceptionHandler)
        }
      }
    }
  }

  lazy val requestMentionsRoute = {
    get {
      path("RequestMentions" / IntNumber) { index =>
        {
          complete {
            //println("requesting")
            (assignerList(index % NUMBEROFSERVERASSIGNERS) ? requestMentions(index)).mapTo[spray.json.JsArray]
              .map(s => s"$s")
          }
          //onFailure(myExceptionHandler)
        }
      }
    }
  }
  lazy val requestHashTagRoute = {
    get {
      path("RequestHashTag" / PathElement) { hashTag: String =>
        {
          println("hello mugdha")
          val tweetFuture = (assignerList(0) ? requestHashTag(hashTag)).mapTo[spray.json.JsArray]
            .map(s => s"$s")
          complete(tweetFuture)
        }
      }
    }
  }

  lazy val getFollowersListRoute = {
    get {
      path("getFollowersList" / IntNumber) { index =>
        {
          complete {
            //println("requesting")
            (assignerList(index % NUMBEROFSERVERASSIGNERS) ? requestFollowersList(index)).mapTo[spray.json.JsArray]
              .map(s => s"$s")
          }
        }
      }
    }
  }

   lazy val getMyMsgs = {
    get {
      path("getMyMsgs" / IntNumber) { index =>
        {
          complete {
            (assignerList(index % NUMBEROFSERVERASSIGNERS) ? requestMsgsList(index)).mapTo[spray.json.JsArray]
              .map(s => s"$s")
          }
        }
      }
    }
  }
  
  val tweetRoute =
    path("tweet") {
      post {
        entity(as[JObject]) { tweetObj =>
          complete {
            var jtweet = tweetObj.extract[Tweet]
            var text = jtweet.text
            var uid = jtweet.uid
            var mentions = jtweet.mentions
            var hashTag = jtweet.hashTag
            tweetIDCtr += 1
            tweetIDCumulativeCtr += 1
            assignerList(uid % NUMBEROFSERVERASSIGNERS) ! tweetReceived(text, uid, mentions, hashTag)
            text.toString()
          }
        }
      }
    }
  
  val message =
    path("message") {
      post {
        entity(as[JObject]) { tweetObj =>
          complete {
            var jtweet = tweetObj.extract[message]
            var src = jtweet.src
            var dest = jtweet.dest
            var msg = jtweet.msg
 
            assignerList(dest % NUMBEROFSERVERASSIGNERS) ! msgReceived(src, dest, msg)
            msg.toString()
          }
        }
      }
    }
  
  
  lazy val helloo = {
    get {
      path("hello") {
        complete {
          println("Welcome to Twitter Project! \nTeam members: \n Mugdha (UFID:54219168) \n Palak Shah (UFID:55510961)\n Divya Ramachandran (UFID:46761308)")
          "Welcome to Twitter Project! Team members: Mugdha (UFID:54219168) Palak Shah (UFID:55510961) Divya Ramachandran (UFID:46761308)"
        }
      }
    }
  }

  def hello {
    println("helo")
  }

}