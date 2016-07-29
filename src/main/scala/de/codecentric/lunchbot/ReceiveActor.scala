package de.codecentric.lunchbot

import akka.actor.{Actor, ActorRef, Props}
import akka.http.scaladsl.model.ws.TextMessage
import de.codecentric.lunchbot.ReceiveActor.{SendMessage, SlackEndpoint}
import io.circe.syntax._

class ReceiveActor(defaultChannel: String, self: User) extends Actor {
  var outgoingId = 0
  var slackEndpoint: Option[ActorRef] = None

  def freshId() = {
    outgoingId+=1
    outgoingId
  }

  override def receive: Receive = {
    case SlackEndpoint(ref) => slackEndpoint = Some(ref)
    case IncomingSlackMessage("message", _, text, _, user)
      if text.contains(self.id) || text.contains(self.name) =>
      sendMessage(s"Hello $user")
    case msg: IncomingSlackMessage if msg.`type` == "message" =>
      sendMessage(s"ECHO: ${msg.text}")
    case msg: SendMessage => sendMessage(msg.text, msg.channel)
  }

  def sendMessage(text: String, channel: String = defaultChannel): Unit = {
    val outMsg = OutgoingSlackMessage(freshId(), defaultChannel, text)
    slackEndpoint.foreach(_ ! TextMessage(outMsg.asJson.toString))
  }
}

object ReceiveActor {
  def props(defaultChannel:String, self: User) = Props(new ReceiveActor(defaultChannel, self))

  case class SlackEndpoint(actorRef: ActorRef)

  case class SendMessage(text: String, channel: String)

}