package de.codecentric.lunchbot

import akka.Done
import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import com.typesafe.config.{Config, ConfigFactory}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Json

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

case class WebsocketUrl(value: String) extends AnyVal

object LunchBot extends App with CirceSupport {
  val config: Config = ConfigFactory.load()

  val token: String = config.getString("lunchbot.token")

  def websocketUrl: Future[Option[WebsocketUrl]] = for {
    response <- Http().singleRequest(HttpRequest(uri = s"https://slack.com/api/rtm.start?token=$token"))
    json <- Unmarshal(response.entity).to[Json]
  } yield {
    json.cursor.downField("url").get.as[String].toOption.map(WebsocketUrl(_))
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

  val result: Future[(ActorRef, Future[Done])] = for {
    urlOpt <- websocketUrl
    if urlOpt.isDefined
  } yield {
    slackSource(urlOpt.get).toMat(Sink.foreach(println))(Keep.both).run()
  }

  val (actorRef, done) = Await.result(result, Duration.Inf)
  Await.result(done, Duration.Inf)

  println("Press enter to exit")
  System.in.read()

  materializer.shutdown()
  Await.result(system.terminate(), Duration.Inf)
}
