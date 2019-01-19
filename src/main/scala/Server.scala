import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContextExecutor, Future}

object Server extends App {
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val CORSHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With")
  )

  val route = post {
    path("credit-card") {
      complete(HttpResponse(StatusCodes.OK, CORSHeaders))
    } ~
    path("internet-bank") {
      complete(Future({
        Thread.sleep(3000)
        HttpResponse(StatusCodes.OK, CORSHeaders)
      }))
    } ~
    path("ask") {
      complete(StatusCodes.Forbidden)
    }
  }

  val port = 8081
  val bindingFuture = Http().bindAndHandle(route, "localhost", port)

  println("Server is up on port " + port)
}
