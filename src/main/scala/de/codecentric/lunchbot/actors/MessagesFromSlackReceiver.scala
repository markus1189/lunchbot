package de.codecentric.lunchbot.actors

import akka.actor.{Actor, ActorRef, Props, ActorLogging}
import akka.http.scaladsl.model.ws.TextMessage
import MessagesFromSlackReceiver.{SendMessage, SlackEndpoint}
import de.codecentric.lunchbot.{IncomingSlackMessage, OutgoingSlackMessage, User}
import io.circe.syntax._

import de.codecentric.lunchbot._

class MessagesFromSlackReceiver(defaultReceiver: SlackId, self: User) extends Actor with ActorLogging {
  var outgoingId = 0
  var slackEndpoint: Option[ActorRef] = None

  def freshId() = {
    outgoingId += 1
    outgoingId
  }

  override def receive: Receive = {
    case SlackEndpoint(ref) => slackEndpoint = Some(ref)
    case m@IncomingSlackMessage("message", _, text, _, sender, senderUser) if text.contains(s"<@${self.id.value}>") =>
      log.debug(s"Received message: $m")
//      sendMessage(s"Hello ${senderUser.map(_.displayName).getOrElse(sender)}", "D0BLAU7PW")
    case msg: IncomingSlackMessage if msg.user != self.id =>
      sendMessage(s"ECHO: ${msg.text}")
    case msg: SendMessage =>
      sendMessage(msg.text, msg.channel)
  }

  def sendMessage(text: String, receiver: SlackId = defaultReceiver): Unit = {
    val outMsg = OutgoingSlackMessage(freshId(), receiver, text)
    log.debug(s"Sending message: $outMsg")
    slackEndpoint.foreach(_ ! TextMessage(outMsg.asJson.toString))
  }
}

object MessagesFromSlackReceiver {
  // TODO: add pmChannels to ctor
  def props(defaultChannel: SlackId, self: User) = Props(new MessagesFromSlackReceiver(defaultChannel, self))

  case class SlackEndpoint(actorRef: ActorRef)

  case class SendMessage(text: String, channel: SlackId)

}
