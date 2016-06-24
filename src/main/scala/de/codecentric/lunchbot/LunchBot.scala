package de.codecentric.lunchbot

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, WebSocketRequest, WebSocketUpgradeResponse}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Json

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.StdIn


object LunchBot extends App with CirceSupport {
  implicit val system = ActorSystem("LunchBot-System")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Say hello to akka-http</h1>"))
      }
    }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "https://slack.com/api/rtm.start?<token>"))
  val response = Await.result(responseFuture, Duration.Inf)
  val unmarshalled: Future[Json] = Unmarshal(response.entity).to[Json]
  val json: Json = Await.result(unmarshalled, Duration.Inf)

  val wss: String = json.cursor.downField("url").get.as[String].getOrElse(sys.error("error"))

  println(wss)

  val webSocketFlow: Flow[Message, Message, Future[WebSocketUpgradeResponse]] = Http().webSocketClientFlow(WebSocketRequest(wss))

  val (upgradeResponse, closed) =
    Source.empty
      .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
      .toMat(Sink.foreach(println))(Keep.both) // also keep the Future[Done]
      .run()

  val connected = upgradeResponse.flatMap { upgrade =>
    if (upgrade.response.status == StatusCodes.OK) {
      Future.successful(Done)
    } else {
      throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
    }
  }

  Await.result(connected, Duration.Inf)

  //TODO ascii art ftw (i´m the lunchbot baby!)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())
}
