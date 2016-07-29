package de.codecentric.lunchbot

import akka.Done
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import cats.data.Xor
import com.typesafe.config.{Config, ConfigFactory}
import de.codecentric.lunchbot.ReceiveActor.SlackEndpoint
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder.Result
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.{DecodingFailure, Json, parse}

import scala.concurrent.Future

case class WebsocketUrl(value: String) extends AnyVal

object LunchBot extends App with CirceSupport {
  val config: Config = ConfigFactory.load()

  val token: String = config.getString("lunchbot.token")
  val defaultChannel: String = config.getString("lunchbot.default_channel")

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

  val receiveActor = system.actorOf(ReceiveActor.props(defaultChannel))

  val result: Future[Xor[DecodingFailure, (ActorRef, Future[Done])]] = for {
    handshakeResult <- handshake
  } yield {
    handshakeResult match {
      case Xor.Left(error) => Xor.Left(error)
      case Xor.Right(handshake) =>
        val translator = system.actorOf(Props(new Translator(handshake, receiveActor)))
        Xor.Right(slackSource(WebsocketUrl(handshake.url)).
          toMat(sink(translator))(Keep.both).run())
    }
  }

  def sink(incomingMsgReceiver: ActorRef): Sink[Message, Future[Done]] = Sink.foreach {
    case TextMessage.Strict(text) =>
      parse.parse(text).map(JsonUtils.snakeCaseToCamelCaseAll) match {
        case Xor.Left(_) => ()
        case Xor.Right(json) =>
          json.as[IncomingSlackMessage].foreach(msg => incomingMsgReceiver ! msg)
      }
    case _ => ()
  }

  val termination = result.flatMap {
    case Xor.Left(err) => Future.successful(println(s"Handshake failed: $err"))
    case Xor.Right((actorRef, done)) =>

      receiveActor ! SlackEndpoint(actorRef)
      actorRef ! TextMessage(OutgoingSlackMessage(42, defaultChannel, "I'm active").asJson.toString)

      println("Press enter to exit")
      System.in.read()
      materializer.shutdown()
      system.terminate()
  }
}