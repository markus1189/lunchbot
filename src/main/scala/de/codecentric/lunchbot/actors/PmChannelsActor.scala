package de.codecentric.lunchbot.actors

import akka.pattern.pipe
import akka.actor._
import akka.http.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream._
import cats.data.{ Xor, XorT }
import cats.std.future._
import cats.syntax.applicativeError._
import de.codecentric.lunchbot.SlackHandShake
import de.codecentric.lunchbot.actors.PmChannelsActor._
import de.codecentric.lunchbot.actors.WebApiHandler.Im
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder.Result
import io.circe.generic.auto._
import io.circe.{ DecodingFailure, _ }

import de.codecentric.lunchbot._
import scala.concurrent.Future

class PmChannelsActor(handShake: SlackHandShake, webApiHandler: ActorRef) extends Actor {
  def receive = {
    case ResolveChannel(userId) =>
      handShake.ims.find(_.user == userId) match {
        case Some(channel) =>
          sender() ! channel
        case None =>
          webApiHandler ! Im.Open(userId, sender())
      }
  }
}

object PmChannelsActor {
  def props(handShake: SlackHandShake, webApiHandler: ActorRef) = Props(new PmChannelsActor(handShake, webApiHandler))

  case class ResolveChannel(userId: UserId)
}

class WebApiHandler(slackToken: String) extends Actor {
  def receive = {
    case Im.Open(userId, forwardTo) => ???
  }
}
object WebApiHandler {
  def props(slackToken: String) = Props(new WebApiHandler(slackToken))

  object Im {
    case class Open(userId: UserId, forwardTo: ActorRef)
    case class Opened(dmc: DirectMessageChannel)
  }
}
