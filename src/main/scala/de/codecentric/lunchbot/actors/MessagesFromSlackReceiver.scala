package de.codecentric.lunchbot.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.ws.TextMessage
import MessagesFromSlackReceiver.{SendMessage, SlackEndpoint}
import de.codecentric.lunchbot.{IncomingSlackMessage, OutgoingSlackMessage, User}
import io.circe.syntax._

class MessagesFromSlackReceiver(defaultChannel: String, self: User) extends Actor {
  var outgoingId = 0
  var slackEndpoint: Option[ActorRef] = None

  def freshId() = {
    outgoingId+=1
    outgoingId
  }

  override def receive: Receive = {
    case SlackEndpoint(ref) => slackEndpoint = Some(ref)
    case IncomingSlackMessage("message", _, text, _, sender,_)
      if text.contains(s"<@${self.id}>") || text.contains(s"<@${self.name}>") =>
      sendMessage(s"Hello $sender")
    case msg: IncomingSlackMessage if msg.`type` == "message" && msg.user != self.id =>
      sendMessage(s"ECHO: ${msg.text}")
    case msg: SendMessage => sendMessage(msg.text, msg.channel)
  }

  def sendMessage(text: String, channel: String = defaultChannel): Unit = {
    val outMsg = OutgoingSlackMessage(freshId(), defaultChannel, text)
    slackEndpoint.foreach(_ ! TextMessage(outMsg.asJson.toString))
  }
}

object MessagesFromSlackReceiver {
  def props(defaultChannel:String, self: User) = Props(new MessagesFromSlackReceiver(defaultChannel, self))

  case class SlackEndpoint(actorRef: ActorRef)

  case class SendMessage(text: String, channel: String)

}