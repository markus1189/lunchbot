package de.codecentric.lunchbot.actors

import akka.actor.{Actor, ActorRef, Props}
import de.codecentric.lunchbot.SlackHandShake
import de.codecentric.lunchbot.actors.PmChannelsActor._
import de.codecentric.lunchbot.actors.WebApiHandler.OpenIMChannel

class PmChannelsActor(handShake: SlackHandShake, webApiHandler: ActorRef) extends Actor {
  def receive = {
    case ResolveChannel(userId) =>
      handShake.ims.find(_.user == userId) match {
        case Some(channel) => sender() ! channel
        case None => webApiHandler ! OpenIMChannel(userId, sender())
      }
  }
}
object PmChannelsActor {
  def props(handShake: SlackHandShake, webApiHandler: ActorRef) = Props(new PmChannelsActor(handShake, webApiHandler))

  case class ResolveChannel(userId: String)
}

class WebApiHandler(slackToken: String) extends Actor {
  def receive = {
    case OpenIMChannel(userId, forwardTo) => ???
  }
}
object WebApiHandler {
  def props(slackToken: String) = Props(new WebApiHandler(slackToken))

  case class OpenIMChannel(userId: String, forwardTo: ActorRef)
}
