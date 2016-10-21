package de.codecentric.lunchbot.actors

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import cats.data.Xor
import de.codecentric.lunchbot.{ChannelId,UserId,DirectMessageChannel,DmId}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder.Result
import io.circe.Json

import scala.concurrent.Future

trait SlackApi {
  def openPmChannel(userId: UserId): Future[Option[DirectMessageChannel]]
}

class SlackWsApi(token: Token, actorSystem: ActorSystem) extends SlackApi with CirceSupport {
  implicit val as = actorSystem
  implicit val mat = ActorMaterializer()
  implicit val ex = actorSystem.dispatcher

  override def openPmChannel(userId: UserId): Future[Option[DirectMessageChannel]] = for {
    // TODO use correct url!
    response <- Http().singleRequest(HttpRequest(uri = s"https://slack.com/api/rtm.start?token=$token"))
    json <- Unmarshal(response.entity).to[Json]
  } yield {
    val result: Option[Result[String]] = for {
      channel <- json.cursor.downField("channel")
      id <- channel.downField("id")
    } yield id.as[String]

      result match {
        case Some(Xor.Right(id)) => Some(DirectMessageChannel(DmId(id),userId))
        case _ => None
      }
  }

}

case class Token(value: String) extends AnyVal
