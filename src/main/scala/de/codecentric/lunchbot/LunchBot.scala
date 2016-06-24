package de.codecentric.lunchbot

import java.util.concurrent.CountDownLatch

import akka.Done
import akka.actor.{ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest, WebSocketUpgradeResponse}
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

  val responseFuture: Future[HttpResponse] = Http().singleRequest(HttpRequest(uri = "https://slack.com/api/rtm.start?token=<TOKEN>"))
  val response = Await.result(responseFuture, Duration.Inf)
  val unmarshalled: Future[Json] = Unmarshal(response.entity).to[Json]
  val json: Json = Await.result(unmarshalled, Duration.Inf)

  val wss: String = json.cursor.downField("url").get.as[String].getOrElse(sys.error("error"))

  println(wss)

  val webSocketFlow: Flow[Message, Message, Future[WebSocketUpgradeResponse]] = Http().webSocketClientFlow(WebSocketRequest(wss))

  val (upgradeResponse: Future[WebSocketUpgradeResponse], closed: Future[Done]) =
    Source.repeat(TextMessage("hello"))
      .viaMat(webSocketFlow)(Keep.right) // keep the materialized Future[WebSocketUpgradeResponse]
      .toMat(Sink.foreach(println))(Keep.both) // also keep the Future[Done]
      .run()

  val r: WebSocketUpgradeResponse = Await.result(upgradeResponse, Duration.Inf)
  println(r)

  val c = Await.result(closed, Duration.Inf)
  println("CLOSED: " + c)

  println("Press enter to exit")
  System.in.read()

  materializer.shutdown()
  Await.result(system.terminate(), Duration.Inf)
}
