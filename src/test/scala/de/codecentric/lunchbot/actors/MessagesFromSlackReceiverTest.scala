package de.codecentric.lunchbot.actors

import akka.actor.ActorSystem
import akka.http.scaladsl.model.ws.TextMessage
import akka.testkit.{TestActorRef, TestKit}
import de.codecentric.lunchbot.actors.MessagesFromSlackReceiver.SlackEndpoint
import de.codecentric.lunchbot.{IncomingSlackMessage, User}
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration._

class MessagesFromSlackReceiverTest extends TestKit(ActorSystem("test")) with FlatSpecLike with Matchers {

  "A Receiver Actor" should "receive Messages from default chat channel" in {
    val receiversUser = User("1", "lunchbot", None, Some(true))
    val defaultChat = "default chat channel"
    val receiver = TestActorRef(MessagesFromSlackReceiver.props(defaultChat, receiversUser))
    receiver ! SlackEndpoint(testActor)

    val messageSendingUser = User("2", "Message Sender Dude", None, None)
    receiver ! IncomingSlackMessage("message", defaultChat, "Hey @lunchbot!", "42", messageSendingUser.displayName)

    expectMsgPF(1.seconds) {
      case TextMessage.Strict(text) => {
        text.contains(messageSendingUser.displayName)
      }
    } shouldBe true
  }

}
