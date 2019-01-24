import java.security.MessageDigest

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.{ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{entity, _}
import akka.http.scaladsl.server.{Directive0, Route}
import akka.http.scaladsl.server.directives.Credentials
import akka.stream.ActorMaterializer
import cats.effect.IO
import com.typesafe.scalalogging.Logger
import domain._
import domain.json.Decoding._
import domain.json.Encoding._
import io.circe.syntax._
import io.circe.{Encoder, Json}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

class Server(config: Config)
            (implicit val executionContext: ExecutionContextExecutor, system: ActorSystem, log: Logger) {

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import util.CirceMarshalling._

  val CORSHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With", "Content-Range"),
    `Access-Control-Expose-Headers`("Content-Range")
  )

  def handleCORS(route: Route): Route =
    respondWithHeaders(CORSHeaders) {
      options {
        import HttpMethods._
        complete(HttpResponse(StatusCodes.OK).
          withHeaders(`Access-Control-Allow-Methods`(OPTIONS, POST, GET, PATCH)))
      } ~
      route
    }

  val db = new Database(config.database)
  val security = new Security(db)

  implicit def toResponseMarshaller[T: Encoder](implicit m: ToEntityMarshaller[T]) =
    m.map(entity => HttpResponse(StatusCodes.OK, entity = entity))

  val failure = complete(HttpResponse(StatusCodes.InternalServerError))
  val badRequest = complete(HttpResponse(StatusCodes.BadRequest))
  val forbidden = complete(HttpResponse(StatusCodes.Forbidden))

  def IOToRoute[T](entity: IO[T])(implicit m: ToResponseMarshaller[T]) =
    onComplete(entity.unsafeToFuture()) {
      case Success(obj) =>
        log.info("Database transaction successful")
        complete(obj)
      case Failure(e) =>
        log.error("Database transaction resulted in error:\n" + e.toString)
        failure
    }

  def rangeRoute[T](getRange: RangeRequest => IO[List[T]], getTotal: IO[Int])(implicit encoder: Encoder[T]) =
    get {
      parameterMap {
        case RangeRequest(request) =>
            IOToRoute(
              for {
                range <- getRange(request)
                number <- getTotal
                rangeHeader = `Content-Range`(RangeUnits.Other("records"),
                  ContentRange(request.start, if(request.end > number) number - 1 else request.end, number))
              } yield HttpResponse(
                headers = List(rangeHeader),
                entity = HttpEntity(ContentTypes.`application/json`, range.asJson.noSpaces))
            )
        case _ => badRequest
      }
    }

  val route =
    handleCORS {
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
                  complete(HttpResponse(StatusCodes.OK, List(disposition), entity))
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
      pathPrefix("admin") {
        Route.seal {
          authenticateBasicAsync("admin", security.authenticator) { _ =>
            path("payments") {
              rangeRoute(db.getPaymentRange, db.getPaymentRowNumber) ~
              patch {
                entity(as[SetSafetyRequest]) { param =>
                  IOToRoute(db.changeSafety(param))
                }
              }
            } ~
            path("requests") {
              rangeRoute(db.getRequestRange, db.getRequestRowNumber)
            } ~
            complete(HttpResponse(StatusCodes.OK))
          }
        }
      } ~
      extractRequest { request =>
        extractUnmatchedPath { path =>
          log.info("Unrecognized request at " + path + ": " + request)
          badRequest
        }
      }
    }

  def start(): Future[Http.ServerBinding] = {
    Http().bindAndHandle(route, config.server.interface, config.server.port)
  }
}
