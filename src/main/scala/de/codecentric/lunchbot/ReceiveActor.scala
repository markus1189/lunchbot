package de.codecentric.lunchbot

import akka.actor.{ActorRef, Props, Actor}
import akka.http.scaladsl.model.ws.TextMessage
import de.codecentric.lunchbot.ReceiveActor.SlackEndpoint
import io.circe.syntax._

class ReceiveActor(defaultChannel: String) extends Actor {

  var outgoingId = 0
  var slackEndpoint: Option[ActorRef] = None

  def freshId() = {
    outgoingId+=1
    outgoingId
  }

  override def receive: Receive = {
    case SlackEndpoint(ref) => slackEndpoint = Some(ref)
    case msg: IncomingSlackMessage if msg.`type` == "message" =>

      val outMsg = OutgoingSlackMessage(freshId(), defaultChannel, s"ECHO: ${msg.text}")
      slackEndpoint.foreach(_ ! TextMessage(outMsg.asJson.toString))
  }
}

object ReceiveActor {
  def props(defaultChannel:String) = Props(new ReceiveActor(defaultChannel))

  case class SlackEndpoint(actorRef: ActorRef)
}