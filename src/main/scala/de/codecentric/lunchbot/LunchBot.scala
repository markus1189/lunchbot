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
import io.circe.Decoder.Result
import io.circe.{DecodingFailure, Json, parse}
import io.circe.generic.auto._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class WebsocketUrl(value: String) extends AnyVal

object LunchBot extends App with CirceSupport {
  val config: Config = ConfigFactory.load()

  val token: String = config.getString("lunchbot.token")

  def handshake: Future[Result[SlackHandShake]] = for {
    response <- Http().singleRequest(HttpRequest(uri = s"https://slack.com/api/rtm.start?token=$token"))
    json <- Unmarshal(response.entity).to[Json]
    camelCaseJson = JsonUtils.snakeCaseToCamelCaseAll(json)
  } yield {
    camelCaseJson.as[SlackHandShake]
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
        case Xor.Right(json) => (println(json)) // TODO
      }
    case _ => ()
  }


  val result: Future[Xor[DecodingFailure, (ActorRef, Future[Done])]] = for {
    hs <- handshake
  } yield {
    hs match {
      case Xor.Left(error) => Xor.Left(error)
      case Xor.Right(success) => Xor.Right(slackSource(WebsocketUrl(success.url)).toMat(sink)(Keep.both).run())
    }
  }

  val termination = result.flatMap {
    case Xor.Left(err) => Future.successful(println(s"Handshake failed: $err"))
    case Xor.Right((actorRef, done)) =>
      println("Press enter to exit")
      System.in.read()
      materializer.shutdown()
      system.terminate()
  }


}