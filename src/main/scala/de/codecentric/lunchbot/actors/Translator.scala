package de.codecentric.lunchbot.actors

import akka.actor.{Actor, ActorRef}
import de.codecentric.lunchbot.{IncomingSlackMessage, SlackHandShake, User}

class Translator(handshake: SlackHandShake, forward: ActorRef) extends Actor {
  override def receive: Receive = {
    case msg: IncomingSlackMessage => forward ! expandUser(msg)
  }

  private def expandUser(msg: IncomingSlackMessage): IncomingSlackMessage = {
    msg.copy(translatedUser = handshake.users.find(_.id == msg.user))
  }
}
