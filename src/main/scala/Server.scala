import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.stream.ActorMaterializer
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import domain._
import domain.json.Decoding._
import domain.json.Encoding._
import io.circe.{Encoder, Json}
import domain.VATOptions.VATOptions

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
        log.info("Database transaction successful")
        complete(obj)
      case Failure(e) =>
        log.error("Database transaction resulted in error:\n" + e.toString)
        failure
    }

  val route =
    path("payment") {
      post {
        entity(as[Payment]) { payment =>
          IOToRoute(db.addPayment(payment))
        }
      } ~
      get {
        parameterMap {
          case InternetBankPayment(request) =>
            FileCreator.getPDF(request) match {
              case Some(bytes) =>
                val entity = HttpEntity(ContentType(MediaTypes.`application/pdf`), bytes)
                val disposition = `Content-Disposition`(
                  ContentDispositionTypes.attachment,
                  Map("filename" -> "bank.pdf"))
                complete(HttpResponse(StatusCodes.OK, disposition  +: CORSHeaders, entity))
              case None => failure
            }
          case _ => badRequest
        }
      }
    } ~
    path("request") {
      post {
        entity(as[Request]) { request =>
          IOToRoute(db.addRequest(request))
        }
      }
    } ~
    path("admin") {
      path("payments") {
        get {
          parameterMap {
            case RangeRequest(request) =>
              IOToRoute(
                for {
                  range <- db.getPaymentRange(request)
                  number <- db.getPaymentRowNumber
                } yield RangeResponse(number, range))
            case _ => badRequest
          }
        } ~
        patch {
          entity(as[SetSafetyRequest]) { param =>
            IOToRoute(db.changeSafety(param))
          }
        }
      } ~
      path("requests") {
        get {
          parameterMap {
            case RangeRequest(request) =>
              IOToRoute(
                for {
                  range <- db.getRequestRange(request)
                  number <- db.getRequestRowNumber
                } yield RangeResponse(number, range))
            case _ => badRequest
          }
        }
      }
    } ~
    extractUnmatchedPath { path =>
      entity(as[Json]) { json =>
        log.info("Unrecognized json request at " + path + ": " + json.noSpaces)
        badRequest
      } ~
      extractRequest { request =>
        log.info("Not a json request at " + path + ": " + request)
        badRequest
      }
    }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, config.server.interface, config.server.port)
  }
}
