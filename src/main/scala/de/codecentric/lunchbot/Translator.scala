package de.codecentric.lunchbot

import akka.actor.{Actor, ActorRef}

class Translator(handshake: SlackHandShake, forward: ActorRef) extends Actor {
  override def receive: Receive = {
    case msg: IncomingSlackMessage => forward ! translateMessage(msg)
  }

  private def translateMessage(msg: IncomingSlackMessage): IncomingSlackMessage = {
    msg.copy(user = translateUser(msg.user))
  }

  def translateUser(userId: String): String = {
    val userOption: Option[User] = handshake.users.find(_.id == userId)
    val realNameOption: Option[String] = userOption.flatMap(_.realName)
    val nameOption: Option[String] = userOption.map(_.name)

    (realNameOption orElse nameOption).getOrElse(userId)
  }
}
