package de.codecentric.lunchbot.actors

import akka.actor.{Actor, ActorRef}
import de.codecentric.lunchbot.{IncomingSlackMessage, SlackHandShake, User}

class Translator(handshake: SlackHandShake, forward: ActorRef) extends Actor {
  override def receive: Receive = {
    case msg: IncomingSlackMessage => forward ! translateMessage(msg)
  }

  private def translateMessage(msg: IncomingSlackMessage): IncomingSlackMessage = {
    msg.copy(user = translateUser(msg.user))
  }

  def translateUser(userId: String): String = {
    val userOption: Option[User] = handshake.users.find(_.id == userId)
    userOption.map(_.displayName).getOrElse(userId)
  }
}
