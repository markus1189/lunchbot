package de.codecentric.lunchbot

import actors.MessagesFromSlackReceiver.SlackEndpoint
import actors._
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
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Decoder.Result
import io.circe.generic.auto._
import io.circe.{DecodingFailure, Json, parse}

import scala.concurrent.Future

case class WebsocketUrl(value: String) extends AnyVal

object LunchBot extends App with CirceSupport {
  val config: Config = ConfigFactory.load()

  val token: String = config.getString("lunchbot.token")
  val defaultChannel: SlackId = ChannelId(config.getString("lunchbot.default_channel"))
  val startupMessage: String = config.getString("lunchbot.startup_message")

  def handshake: Future[Result[SlackHandShake]] = for {
    response <- Http().singleRequest(HttpRequest(uri = s"https://slack.com/api/rtm.start?token=$token"))
    json <- Unmarshal(response.entity).to[Json]
    camelCaseJson = JsonUtils.snakeCaseToCamelCaseAll(json)
    isOk = camelCaseJson.hcursor.acursor.downField("ok").as[Boolean]
  } yield {
    isOk.flatMap {
      case ok =>
        if (ok) {
          camelCaseJson.as[SlackHandShake]
        } else {
          Xor.Left(DecodingFailure(s"Error with returned json:\n${camelCaseJson}", List()))
        }
    }
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

  val result: Future[Xor[DecodingFailure, BootResult]] = for {
    handshakeResult <- handshake
  } yield {
    handshakeResult match {
      case Xor.Left(error) =>
        Xor.Left(error)
      case Xor.Right(handshake) =>
        val receiveActor = system.actorOf(MessagesFromSlackReceiver.props(defaultChannel, handshake.self))
        val translator = system.actorOf(Props(new Translator(handshake, receiveActor)))
        val (actorRef, done) = slackSource(WebsocketUrl(handshake.url)).
          toMat(sink(translator))(Keep.both).run()
        Xor.Right(BootResult(SlackEndpoint(actorRef), done, receiveActor))
    }
  }

  def sink(receiver: ActorRef): Sink[Message, Future[Done]] = Sink.foreach {
    case TextMessage.Strict(text) =>
      parse.parse(text).map(JsonUtils.snakeCaseToCamelCaseAll) match {
        case Xor.Left(_) => ()
        case Xor.Right(json) =>
          json.as[IncomingSlackMessage].foreach(msg => receiver ! msg)
      }
    case _ => ()
  }

  def shutDown() = {
    materializer.shutdown()
    system.terminate()
  }

  result.map {
    case Xor.Left(err) =>
      println(s"Handshake failed: $err")
    case Xor.Right(bootResult) =>
      bootResult.receiverRef ! bootResult.slackEndpoint
      bootResult.receiverRef ! MessagesFromSlackReceiver.SendMessage(startupMessage, defaultChannel)

      println("Press enter to exit")
      System.in.read()
  }.foreach(_ => shutDown())
}
