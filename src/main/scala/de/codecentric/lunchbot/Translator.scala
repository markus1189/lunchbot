package de.codecentric.lunchbot

import akka.actor.{Actor, ActorRef}

class Translator(handshake: SlackHandShake, forward: ActorRef) extends Actor {
  private val idRegex = """<@(\w{9})>""".r

  override def receive: Receive = {
    case msg: IncomingSlackMessage => forward ! translateMessage(msg)
  }

  private def translateMessage(msg: IncomingSlackMessage): IncomingSlackMessage = {
    msg.copy(user = translateUser(msg.user), text = translateText(msg.text))
  }

  def translateUser(userId: String): String = {
    val userOption: Option[User] = handshake.users.find(_.id == userId)
    val realNameOption: Option[String] = userOption.flatMap(_.realName)
    val nameOption: Option[String] = userOption.map(_.name)

    (realNameOption orElse nameOption).getOrElse(userId)
  }

  def translateText(text: String): String =
    idRegex.replaceAllIn(text, matcher => s"<@${translateUser(matcher.group(1))}>")
}
