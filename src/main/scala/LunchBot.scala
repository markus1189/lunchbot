import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._

import scala.io.StdIn

object LunchBot extends App {


  //TODO Q: implicits vs injection via Guice
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

  //TODO ascii art ftw (i´m the lunchbot baby!)
  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
    .flatMap(_.unbind())
    .onComplete(_ => system.terminate())


}
