import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import cats.effect.IO
import domain.{Payment, Request}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class Server(config: Config) {
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val CORSHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With")
  )

  val db = new Database(config.database)

  val success = complete(HttpResponse(StatusCodes.OK, CORSHeaders))
  val failure = complete(HttpResponse(StatusCodes.InternalServerError, CORSHeaders))

  def insertToRoute[T](result: IO[Int]) =
    onComplete(result.map(i => if(i == 1) success else failure).unsafeToFuture()) {
      case Success(x) => x
      case Failure(_) => failure
    }

  val route = post {
    path("credit-card") {
      entity(as[Payment]) { payment =>
        insertToRoute(db.addPayment(payment))
      }
    } ~
      path("internet-bank") {
        failure
      } ~
      path("ask") {
        entity(as[Request]) { request =>
          insertToRoute(db.addRequest(request))
        }
      }
  }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, config.server.interface, config.server.port)
  }
}
