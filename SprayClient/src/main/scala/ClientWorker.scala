import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random
import akka.actor.ActorSystem
import scala.collection.mutable.Queue
import spray.client.pipelining._
import java.net.URI
import spray.json._
import spray.httpx.SprayJsonSupport
import spray.json.AdditionalFormats
import org.json4s.Formats
import org.json4s.DefaultFormats
import DefaultJsonProtocol._

class ClientWorker(UID: Int, totalUsers: Long, serIP: String, serPort: String, sys: ActorSystem, cliID: Int) extends SprayJsonSupport with AdditionalFormats with Actor {

  var freq = Math.ceil(totalUsers * scala.math.pow(2, -UID / 2)) //var freq = Math.ceil(totalUsers*scala.math.pow(2, -UID/8))
  var numReq: Int = 0
  var reqRecvd: Int = 0
  val harryPotter: String = "'Filth! Scum! By-products of dirt and vileness! Half-breeds, mutants, freaks, begone from this place!How dare you befoul the house of my fathers -'Tonks apologised over and over again, dragging the huge, heavy troll's leg back off the floor; MrsWeasley abandoned the attempt to close the curtains and hurried up and down the hall, stunning all theother portraits with her wand; and a man with long black hair came charging out of a door facing Harry. 'Shut up, you horrible old hag, shut UP!' he roared, seizing the curtain Mrs Weasley had abandoned. The old woman's face blanched. 'Yoooou!' she howled, her eyes popping at the sight of the man. 'Blood traitor, abomination, shameof my flesh!''I said - shut - UP!' roared the man, and with a stupendous effort he and Lupin managed to force thecurtains closed again. The old woman's screeches died and an echoing silence fell. Panting slightly and sweeping his longdark hair out of his eyes, Harry's godfather Sirius turned to face him. 43'Hello, Harry, ' he said grimly, 'I see you've met my mother. 'Your -?''My dear old mum, yeah, ' said Sirius. 'We've been trying to get her down for a month but we thinkshe put a Permanent Sticking Charm on the back of the canvas. Let's get downstairs, quick, before theyall wake up again. ''But what's a portrait of your mother doing here?' Harry asked, bewildered, as they went through thedoor from the hall and led the way down a flight of narrow stone steps, the others just behind them. 'Hasn't anyone told you? This was my parents' house, ' said Sirius. 'But I'm the last Black left, so it'smine now. I offered it to Dumbledore for Headquarters - about the only useful thing I've been able todo."
  val hashTags = { "#HARRYPOTTER" }  
  val pipeline = sendReceive
  var tweetScheduler:Cancellable = new Cancellable {override def isCancelled: Boolean = false
      override def cancel(): Boolean = false
  }
  var requestScheduler:Cancellable = new Cancellable {override def isCancelled: Boolean = false
      override def cancel(): Boolean = false
  }
  var eventScheduler:Cancellable = new Cancellable {override def isCancelled: Boolean = false
      override def cancel(): Boolean = false
  }
  
  def receive = {

    case "start" => start()
    case "cancelActions" => 
      tweetScheduler.cancel()
      requestScheduler.cancel()
      eventScheduler.cancel()
    case x => println("Default " + UID + " " + x + " " + sender.path)

  }

  def request = {

    val result = pipeline(Get("http://" + serIP + ":" + serPort + "/RequestTimeline/" + UID))
    result.foreach { response =>
    //  println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")

    }

    val resultMentions = pipeline(Get("http://" + serIP + ":" + serPort + "/RequestMentions/" + UID))
    resultMentions.foreach { response =>
    //  println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")
    }

  }

  def getFollowersList = {

    val result = pipeline(Get("http://" + serIP + ":" + serPort + "/getFollowersList/" + UID))
    result.foreach { response => 
    //  println(s"Request completed with status ${response.status} and content:\n${response.entity.asString}")

    }

  }

  def tweet(eventFlag: Boolean) = {
    var start: Int = Random.nextInt(1500)
    var end: Int = 1

    if (eventFlag) {
      end = start + Random.nextInt(137)
    } else {
      end = start + Random.nextInt(120) + 3
    }
    var mentionUser: Int = -1
    var text: String = harryPotter.substring(start, end)
    if (UID % 50 == 0) {
      mentionUser = Random.nextInt(totalUsers.toInt);
      var a = "@" + mentionUser + " "
      text = a + text
    }

    var hashTag: String = "" //default value

    if (eventFlag) {
      hashTag = "#HARRYPOTTER"
      val (fst, end) = text.splitAt(text.length() / 2)
      text = fst + hashTag + end
    }

    pipeline(Post("http://" + serIP + ":" + serPort + "/tweet", s"""{
        "uid": $UID,
        "text": "$text",
        "mentions" : $mentionUser,
        "hashTag"  : "$hashTag"
    }""".asJson.asJsObject))

  }
  
  def message(destID:Int) = { 
        pipeline(Post("http://" + serIP + ":" + serPort + "/message", s"""{
        "src": $UID,
        "dest": $destID,
        "msg": "Hi, I am sending you a message"
    }""".asJson.asJsObject))
  }
  
def getMyMsgs = {

    val result = pipeline(Get("http://" + serIP + ":" + serPort + "/getMyMsgs/" + UID))
    result.foreach { response =>
      println(s"Messages Request completed with status ${response.status} and content:\n${response.entity.asString}")
    }
  }
  
  def start() = {

    if (freq != 1) {
      var x = 0.0
      if(freq != 0) { 
        x = 10.toDouble / freq.toDouble 
      }
      
      tweetScheduler = context.system.scheduler.schedule(0 seconds, x seconds)(tweet(false))
    } else {
      //println("time / freq = "+ (UID.toDouble/totalUsers.toDouble*10).toInt)
      tweetScheduler = context.system.scheduler.scheduleOnce((UID.toDouble / totalUsers.toDouble * 100.0).toInt seconds)(tweet(false))
    }
    requestScheduler = context.system.scheduler.schedule(UID*10 millis, 5 seconds)(request) //to be fixed
    eventScheduler = context.system.scheduler.scheduleOnce(30 seconds)(tweet(true))
  }
}