import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import cats.effect.IO
import com.microsoft.sqlserver.jdbc.SQLServerException
import com.typesafe.scalalogging.Logger
import domain._
import domain.json.Decoding._
import io.circe.Json

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class Server(config: Config)
            (implicit val executionContext: ExecutionContextExecutor, system: ActorSystem, log: Logger) {

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
  val badRequest = complete(HttpResponse(StatusCodes.BadRequest, CORSHeaders))

  def insertToRoute[T](result: IO[Int]) =
    onComplete(result.flatMap(
      i =>
        if(i == 1) IO(Unit)
        else IO.raiseError(new Exception("No data modified"))
    ).unsafeToFuture()) {
      case Success(_) =>
        log.debug("Data inserted")
        success
      case Failure(_) =>
        log.error("Database transaction resulted in error")
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
    entity(as[Json]) { _ =>
      log.info("Unrecognized json request")
      badRequest
    } ~
    extractRequest { _ =>
      log.info("Error: not a json request")
//      request.entity.toStrict(FiniteDuration(1000, TimeUnit.MILLISECONDS))
//        .map(_.data.utf8String).map(println)
      badRequest
    }
  }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, config.server.interface, config.server.port)
  }
}
