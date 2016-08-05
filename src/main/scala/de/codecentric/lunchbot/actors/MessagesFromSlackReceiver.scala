package de.codecentric.lunchbot.actors

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.ws.TextMessage
import MessagesFromSlackReceiver.{SendMessage, SlackEndpoint}
import de.codecentric.lunchbot.{IncomingSlackMessage, OutgoingSlackMessage, User}
import io.circe.syntax._

class MessagesFromSlackReceiver(defaultReceiver: String, self: User) extends Actor {
  var outgoingId = 0
  var slackEndpoint: Option[ActorRef] = None

  def freshId() = {
    outgoingId += 1
    outgoingId
  }

  override def receive: Receive = {
    case SlackEndpoint(ref) => slackEndpoint = Some(ref)
    case m @ IncomingSlackMessage("message", _, text, _, sender, senderUser) if text.contains(s"<@${self.id}>") =>
      println(m)
      sendMessage(s"Hello ${senderUser.map(_.displayName).getOrElse(sender)}", sender)
    // TODO: clarify how to send direct messages
//      sendMessage(s"Hello ${senderUser.map(_.displayName).getOrElse(sender)}", "D0BLAU7PW")
    case msg: IncomingSlackMessage if msg.user != self.id =>
      sendMessage(s"ECHO: ${msg.text}")
    case msg: SendMessage =>
      sendMessage(msg.text, msg.channel)
  }

  def sendMessage(text: String, receiver: String = defaultReceiver): Unit = {
    val outMsg = OutgoingSlackMessage(freshId(), receiver, text)
    println(outMsg)
    slackEndpoint.foreach(_ ! TextMessage(outMsg.asJson.toString))
  }
}

object MessagesFromSlackReceiver {
  def props(defaultChannel: String, self: User) = Props(new MessagesFromSlackReceiver(defaultChannel, self))

  case class SlackEndpoint(actorRef: ActorRef)

  case class SendMessage(text: String, channel: String)

}