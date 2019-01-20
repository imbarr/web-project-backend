import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import cats.effect.IO
import domain._
import domain.json.Decoding._
import io.circe.Json
import java.nio.charset.StandardCharsets

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class Server(config: Config)
            (implicit val executionContext: ExecutionContextExecutor, implicit val system: ActorSystem) {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import util.CirceMarshalling.unmarshaller

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
      case Failure(e) =>
        println("Error: database failure")
        e.printStackTrace()
        failure
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
    } ~
    entity(as[Json]) { json =>
      println("Error: unrecognized object\n" + json.toString)
      failure
    } ~
    extractRequest { request =>
      println("Error: unknown request\n" + request)
      request.entity.toStrict(FiniteDuration(1000, TimeUnit.MILLISECONDS))
        .map(_.data.utf8String).map(println)
      failure
    }


  }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, config.server.interface, config.server.port)
  }
}
