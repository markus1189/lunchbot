package de.codecentric.lunchbot

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import cats.data.Xor
import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Json
import io.circe.parse
import io.circe.generic.auto._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class WebsocketUrl(value: String) extends AnyVal

object LunchBot extends App with CirceSupport {
  val config: Config = ConfigFactory.load()

  val token: String = config.getString("lunchbot.token")

  def handshake: Future[Option[SlackHandShake]] = for {
    response <- Http().singleRequest(HttpRequest(uri = s"https://slack.com/api/rtm.start?token=$token"))
    json <- Unmarshal(response.entity).to[Json]
    camelCaseJson = JsonUtils.snakeCaseToCamelCaseAll(json)
  } yield {
    camelCaseJson.as[SlackHandShake].toOption
  }

  def slackSource(url: WebsocketUrl): Source[Message, ActorRef] = {
    val webSocketFlow: Flow[Message, Message, Future[WebSocketUpgradeResponse]] =
      Http().webSocketClientFlow(WebSocketRequest(url.value))

    Source.actorRef(1000, OverflowStrategy.dropHead)
      .viaMat(webSocketFlow)(Keep.left)
  }

  implicit val system = ActorSystem("LunchBot-System")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val sink: Sink[Message, Future[Done]] = Sink.foreach {
    case TextMessage.Strict(text) =>
      parse.parse(text).map(JsonUtils.snakeCaseToCamelCaseAll) match {
        case Xor.Left(_) => ()
        case Xor.Right(json) => () // TODO
      }
    case _ => ()
  }

  val result: Future[(ActorRef, Future[Done])] = for {
    hs <- handshake
    if hs.isDefined
  } yield {
    slackSource(WebsocketUrl(hs.get.url)).toMat(sink)(Keep.both).run()
  }

  val (actorRef, done) = Await.result(result, Duration.Inf)
  Await.result(done, Duration.Inf)

  println("Press enter to exit")
  System.in.read()

  materializer.shutdown()
  Await.result(system.terminate(), Duration.Inf)
}