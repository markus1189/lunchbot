package de.codecentric.lunchbot.actors

import akka.http.scaladsl.model.ws.TextMessage
import akka.testkit.TestActorRef
import cats.data.Xor
import de.codecentric.lunchbot.actors.MessagesFromSlackReceiver.SlackEndpoint
import de.codecentric.lunchbot.{IncomingSlackMessage, OutgoingSlackMessage, User}

import scala.concurrent.duration._

import de.codecentric.lunchbot._

class MessagesFromSlackReceiverTest extends ActorTest {

  val lunchBot = User(UserId("U1"), "lunchbot", None, Some(true))
  val defaultChat = ChannelId("default chat channel")
  val messageSendingUser = User(UserId("U2"), "Message Sender Dude", None, None)

  "A Receiver Actor" should "reply to private chat channel" in {
    import io.circe.generic.auto._
    val receiver = TestActorRef(MessagesFromSlackReceiver.props(defaultChat, lunchBot))
    receiver ! SlackEndpoint(testActor)

    receiver ! IncomingSlackMessage("message", defaultChat, "Hey <@U1>!", "42", messageSendingUser.id, Some(messageSendingUser))

    expectMsgPF(1.second) {
      case TextMessage.Strict(text) if text.contains(messageSendingUser.displayName) =>
        val outgoingMessage = for {
          json <- io.circe.parse.parse(text)
          m <- json.as[OutgoingSlackMessage]
        } yield m
        outgoingMessage match {
          case Xor.Left(_) => fail(s"Could not parse OutgoingSlackMessage:\n $text")
          case Xor.Right(m) => m.channel shouldBe messageSendingUser.id
        }
    }
  }

  it should "echo to incoming messages" in {
    val receiver = TestActorRef(MessagesFromSlackReceiver.props(defaultChat, lunchBot))
    receiver ! SlackEndpoint(testActor)

    receiver ! IncomingSlackMessage("message", defaultChat, "woot", "42", messageSendingUser.id, None)

    expectMsgPF(1.second) {
      case TextMessage.Strict(text) if text.contains("ECHO: woot") => ()
    }
  }

  it should "not echo to own messages" in {
    val receiver = TestActorRef(MessagesFromSlackReceiver.props(defaultChat, lunchBot))
    receiver ! SlackEndpoint(testActor)

    receiver ! IncomingSlackMessage("message", defaultChat, "lol :)", "42", lunchBot.id, None)

    expectNoMsg(1.second)

  }

}
