import akka.actor.{ ActorRef, ActorSystem, Props, Actor }
import scala.collection.mutable.HashMap
//import scala.collection.immutable.List
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Queue
import scala.collection.mutable.ArrayBuffer
import spray.json._
import DefaultJsonProtocol._

case class tweetReceived(tweetText: String, ownerID: Int, mention: Int, hashTag: String)
case class updateTimeline(tweetText: String, ownerID: Int, followerSubList: ListBuffer[Int])
case class requestStatus(requestorID: Int)
case class AssignersListSend(ServerAssignService: ArrayBuffer[ActorRef])
case class requestFollowersList(requestorID: Int)
case class requestMsgsList(requestorID: Int)
case class updateMentions(tweetText: String, ownerID: Int, mention: Int)
case class requestMentions(requestorID: Int)
case class updateHashTag(tweetText: String, ownerID: Int, hashTag: String)
case class requestHashTag(hashTag: String)
case class msgReceived(src: Int, dest: Int, msg: String)

class ServerAssigner(numUsers: Int, ac: ActorSystem, myID: Int, NUMBEROFSERVERASSIGNERS: Int, FOLLOWERSLIMIT: Int) extends Actor {

  var tweetTable = HashMap.empty[Int, Tuple2[Int, String]]
  var pagesTable = HashMap.empty[Int, Queue[Tuple2[String, Int]]]
  var followersTable = HashMap.empty[Int, ListBuffer[Int]]
  var mentionsTable = HashMap.empty[Int, Queue[Tuple2[String, Int]]]
  var hashTagTable = HashMap.empty[String, Queue[Tuple2[String, Int]]]
  var msgsTable = HashMap.empty[Int, Queue[Tuple2[Int, String]]]

  // constants
  val DEBUG = false

  // TODO
  // schedule to request for tweets and act on them
  // sendMeTweets

  def receive = {
    case "init" => init(sender)
    case "begin" => println("Initialisation done...")
    case updateTimeline(tweetText: String, ownerID: Int, followerSubList: ListBuffer[Int]) => updateTweetQueue(tweetText: String, ownerID: Int, followerSubList: ListBuffer[Int])
    case requestStatus(requestorID: Int) => returnQueue(requestorID: Int)

    case tweetReceived(tweetText: String, ownerID: Int, mention: Int, hashTag: String) =>
      sendAllFollowersInfo(tweetText: String, ownerID: Int)
      if (mention >= 0) {
        context.actorSelection("akka://TwitterServer/user/" + mention % NUMBEROFSERVERASSIGNERS) ! updateMentions(tweetText, ownerID, mention)
      }
      if (!hashTag.equals(null)) {
        context.actorSelection("akka://TwitterServer/user/" + 0) ! updateHashTag(tweetText, ownerID, hashTag)
      }

     case msgReceived(src: Int, dest: Int, msg: String) =>
      if(!msg.equalsIgnoreCase(""))
        {
    	  updateMsgs(src: Int, dest: Int, msg: String)
        }
      
    case updateMentions(tweetText: String, ownerID: Int, mention: Int) => updateMentionsTable(tweetText, ownerID, mention)
    case requestMentions(requestorID: Int) => returnMentionsQueue(requestorID: Int)
    case updateHashTag(tweetText: String, ownerID: Int, hashTag: String) => updateHashTagTable(tweetText, ownerID, hashTag)
    case requestHashTag(hashTag: String) => returnHashTagQueue(hashTag)
    case requestFollowersList(requestorID: Int) => returnFollowers(requestorID: Int)
    case requestMsgsList(requestorID: Int) => returnMsgs(requestorID: Int)
    case "stop" => context.stop(self)
    case x => println("Default case for Server Assigner:" + x)
  }

  def init(router: ActorRef) {
    var x: Int = 0
    var y: Int = 0
    var rnd = new scala.util.Random

    // followerTable: userID, follower ID
    var followerTable: ListBuffer[ListBuffer[Int]] = new ListBuffer

    for (x <- 0 to numUsers - 1) {

      if ((x % NUMBEROFSERVERASSIGNERS) == myID) {
        // simulate twitter statistics for followers
        // call function returnFollowers()
        var quantity = rnd.nextInt(FOLLOWERSLIMIT)
        var seq: ListBuffer[Int] = new ListBuffer

        for (y <- 0 to quantity) {
          var randomNumber = rnd.nextInt(numUsers - 1)
          while ((randomNumber < 0) || (seq.contains(randomNumber))) {
            randomNumber = rnd.nextInt(numUsers)
          }
          seq += randomNumber

        }
        if (!seq.contains(x)) {
          seq += x
        }

        if (DEBUG) {
          println("for owner : " + x)
          var z = 0
          for (z <- 0 to seq.length) {
            print(" " + z)
          }
        }
        updateFollowersTable(x, seq)
      }
    }
    if (DEBUG) {
      println("Followers table")
      println(followersTable.keys + " " + followersTable.values)
    }
    // router ! "done"
  }

  def sendAllFollowersInfo(tweetText: String, ownerID: Int) {

    var retrievedFollowers: ListBuffer[Int] = followersTable.getOrElse(ownerID, null)

    if (DEBUG) {
      if (retrievedFollowers != null) {
        println("retrievedFollowers for given ownerID " + ownerID)
        //retrievedFollowers.foreach { i =>println(i)
        println(retrievedFollowers.mkString(","))
      }
    }

    var partitionedFollowers = retrievedFollowers.groupBy(x => x % NUMBEROFSERVERASSIGNERS)

    if (DEBUG) {
      println("partitioned followers for given ownerID " + ownerID)
      partitionedFollowers.foreach { i => println(i) }
    }

    // Send the related assigner/worker

    for (x <- 0 to NUMBEROFSERVERASSIGNERS - 1) {
      var temp = partitionedFollowers.getOrElse((x), null)

      if (temp != null) {
        if (DEBUG) {
          println(temp)
        }

        context.actorSelection("akka://TwitterServer/user/" + (x)) ! updateTimeline(tweetText, ownerID, temp)
      }
    }

  }

  def updateTweetQueue(tweetText: String, ownerID: Int, followerSubList: ListBuffer[Int]) {
    //update queue for each user in followerSubList
    //println("I received this follower list : " + followerSubList)
    // for every member of the followerSubList, add new tweet to the queue

    var newMember = new Tuple2(tweetText, ownerID)
    for (member <- followerSubList) {

      //get queue from pagesTable
      var tweetsQ = pagesTable.getOrElse(member, null)

      if (tweetsQ == null) {
        // Add new entry to queue
        tweetsQ = Queue(newMember);

      } else {
        tweetsQ.enqueue(newMember)

        // Limit queue length to 100
        if (tweetsQ.length > 100) {
          tweetsQ.dequeue
        }

      }
      // insert back into hashmap
      pagesTable.put(member, tweetsQ)

      if (DEBUG) {
        println("=" * 20)
        println("inserted text: " + newMember + " into timeline of :" + member)
        println("=" * 20)
      }
    }
  }

  def returnQueue(requestorID: Int) {
    //return the queue for requestorID
    if (DEBUG) {
      println("#" * 200)
      println("inside returnQueue")
    }

    var requiredQ = pagesTable.getOrElse(requestorID, null)

    if (DEBUG) {
      if (requiredQ != null) {
        println("requiredQ")
        println(requiredQ.mkString("\n"))
      }
    }

    //for testing
    //requiredQ = Queue(("a",1),("b",2))
    if (requiredQ != null) {
      val qList: scala.collection.immutable.List[Tuple2[String, Int]] = requiredQ.toList 
      var x = qList.toJson
      //print("printing qList : "+x.prettyPrint)
      sender ! x

      //senderActor ! (requiredQ)
    }
    else
    {
      sender ! List("No tweets on timeline!").toJson
    }
  }

  def updateTweetTable(tweetTableLatest: HashMap[Int, Tuple2[Int, String]]) {
    if (!(tweetTable.equals(tweetTableLatest)) && !(tweetTableLatest.isEmpty)) {
      tweetTable = tweetTableLatest.clone
    }
  }

  def updateFollowersTable(user: Int, followersList: ListBuffer[Int]) {
    if (!(followersList.isEmpty)) {
      followersTable.put(user, followersList)
    }
  }

  def returnFollowers(requestorID: Int) {
    var followersList = followersTable(requestorID).toList //OrElse(requestorID,List(-1))
    sender ! followersList.toJson
  }

    def updateMsgs(src: Int, dest: Int, msg: String) {
     println("inside msgrecvd")
      
      var newMember = new Tuple2(src, msg)

    var msgQ = msgsTable.getOrElse(dest, null)

    if (msgQ == null) {
      // Add new entry to queue
      msgQ = Queue(newMember);

    } else {
      msgQ.enqueue(newMember)

      // Limit queue length to 10
      if (msgQ.length > 10) {
        msgQ.dequeue
      }

    }
    // insert back into hashmap
    msgsTable.put(dest, msgQ)

    if (DEBUG) {
      println("=" * 20)
      println("inserted text: " + newMember + " into msgQ of :" + dest)
      println("=" * 20)
    }
  }
  
   def returnMsgs(requestorID: Int) {
    var msgQ = msgsTable.getOrElse(requestorID,null) //OrElse(requestorID,List(-1))
    if(msgQ != null) {
      var msgList = msgQ.toList
      sender ! msgList.toJson
    }
    else {
      sender ! List("No messages for you!").toJson
    }
  }
    
  def updateMentionsTable(tweetText: String, ownerID: Int, mention: Int) = {
    var newMember = new Tuple2(tweetText, ownerID)

    //get queue from pagesTable
    var mentionsQ = mentionsTable.getOrElse(mention, null)

    if (mentionsQ == null) {
      // Add new entry to queue
      mentionsQ = Queue(newMember);

    } else {
      mentionsQ.enqueue(newMember)

      // Limit queue length to 100
      if (mentionsQ.length > 100) {
        mentionsQ.dequeue
      }

    }
    // insert back into hashmap
    mentionsTable.put(mention, mentionsQ)

    if (DEBUG) {
      println("=" * 20)
      println("inserted text: " + newMember + " into timeline of :" + mention)
      println("=" * 20)
    }
  }

  def returnMentionsQueue(requestorID: Int) {
    //return the queue for requestorID
    if (DEBUG) {
      println("#" * 200)
      println("inside returnQueue")
    }

    var requiredMQ = mentionsTable.getOrElse(requestorID, null)

    if (DEBUG) {
      if (requiredMQ != null) {}
    }

    //for testing
    //requiredQ = Queue(("a",1),("b",2))
    if (requiredMQ != null) {
      val qList: scala.collection.immutable.List[Tuple2[String, Int]] = requiredMQ.toList
      var x = qList.toJson
      //print("printing qList : "+x.prettyPrint)
      sender ! x

      //senderActor ! (requiredQ)
    }else
      sender ! List("No mentions!").toJson
  }

  def updateHashTagTable(tweetText: String, ownerID: Int, hashTag: String) {
    var newMember = new Tuple2(tweetText, ownerID)

    //get queue from pagesTable
    var hashTagQ = hashTagTable.getOrElse(hashTag, null)

    if (hashTagQ == null) {
      // Add new entry to queue
      hashTagQ = Queue(newMember);

    } else {
      hashTagQ.enqueue(newMember)

      /*// Limit queue length to 100
        if (hashTagQ.length > 100) {
          hashTagQ.dequeue
        }*/

    }
    // insert back into hashmap
    hashTagTable.put(hashTag, hashTagQ)

    if (DEBUG) {
      println("=" * 20)
      println("inserted text: " + newMember + " into timeline of :" + hashTag)
      println("=" * 20)
    }
  }

  def returnHashTagQueue(hashTag: String) {
    //return the queue for requestorID
    if (DEBUG) {
      println("#" * 200)
      println("inside returnQueue")
    }

    var requiredHQ = hashTagTable.getOrElse(hashTag, null)

    if (DEBUG) {
      if (requiredHQ != null) {}
    }

    //for testing
    //requiredQ = Queue(("a",1),("b",2))
    if (requiredHQ != null) {
      val qList: scala.collection.immutable.List[Tuple2[String, Int]] = requiredHQ.toList
      var x = qList.toJson
      //print("printing qList : "+x.prettyPrint)
      sender ! x

      //senderActor ! (requiredQ)
    }else
      sender ! List("No hashTags!").toJson
  }
}

object MyJsonProtocol extends DefaultJsonProtocol {
  implicit def ListFormat[A: JsonFormat] = jsonFormat1(scala.collection.immutable.List.apply[A])
}

