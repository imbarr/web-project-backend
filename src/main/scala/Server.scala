import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import domain._
import domain.json.Decoding._
import domain.json.Encoding._
import io.circe.{Encoder, Json}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class Server(config: Config)
            (implicit val executionContext: ExecutionContextExecutor, system: ActorSystem, log: Logger) {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import util.CirceMarshalling._

  val CORSHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With")
  )

  val db = new Database(config.database)

  implicit def toResponseMarshaller[T: Encoder](implicit m: ToEntityMarshaller[T]) =
    m.map(entity => HttpResponse(StatusCodes.OK, CORSHeaders, entity))

  val failure = complete(HttpResponse(StatusCodes.InternalServerError, CORSHeaders))
  val badRequest = complete(HttpResponse(StatusCodes.BadRequest, CORSHeaders))

  def IOToRoute[T](monad: IO[T])(implicit m: ToResponseMarshaller[T]) =
    onComplete(monad.unsafeToFuture()) {
      case Success(obj) =>
        log.debug("Database transaction successfull")
        complete(obj)
      case Failure(e) =>
        log.error("Database transaction resulted in error:\n" + e.toString)
        failure
    }

  val route =
    post {
      path("credit-card") {
        entity(as[Payment]) { payment =>
          IOToRoute(db.addPayment(payment))
        }
      } ~
      path("internet-bank") {
        failure
      } ~
      path("ask") {
        entity(as[Request]) { request =>
          IOToRoute(db.addRequest(request))
        }
      }
      path("payment") {
        entity(as[SetSafetyRequest]) { param =>
          IOToRoute(db.changeSafety(param))
        }
      } ~
      path("fetch-payments") {
        entity(as[RangeRequest]) { param =>
          IOToRoute(db.getPaymentRange(param).flatMap(r =>
            db.getPaymentRowNumber.map(i => RangeResponse(i, r))))
        }
      } ~
      path("fetch-requests") {
        entity(as[RangeRequest]) { param =>
          IOToRoute(db.getRequestRange(param).flatMap(r =>
            db.getRequestRowNumber.map(i => RangeResponse(i, r))))
        }
      }
    } ~
    entity(as[Json]) { json =>
      log.info("Unrecognized json request")
      println(json.spaces4)
      badRequest
    } ~
    extractRequest { request =>
      log.info("Not a json request")
//        request.entity.toStrict(FiniteDuration(1000, TimeUnit.MILLISECONDS))
//          .map(_.data.utf8String).map(println)
      badRequest
    }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, config.server.interface, config.server.port)
  }
}
