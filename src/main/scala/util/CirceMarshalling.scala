package util
import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller, ToResponseMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.util.ByteString
import io.circe._

object CirceMarshalling {
  implicit def unmarshaller[A: Decoder]: FromRequestUnmarshaller[A] = {
    def decode(json: Json) = Decoder[A].decodeJson(json).fold(throw _, identity)
    Unmarshaller.withMaterializer[HttpRequest, ByteString](_ ⇒ implicit mat ⇒ request => request.entity match {
      case HttpEntity.Strict(_, data) ⇒ FastFuture.successful(data)
      case entity                     ⇒ entity.dataBytes.runFold(ByteString.empty)(_ ++ _)
    }).map {
        case ByteString.empty => throw Unmarshaller.NoContentException
        case data             => jawn.parseByteBuffer(data.asByteBuffer).fold(throw _, identity)
      }.map(decode)
  }

  implicit def jsonMarshaller(implicit printer: Printer = Printer.noSpaces): ToEntityMarshaller[Json] = {
    Marshaller.withFixedContentType(ContentTypes.`application/json`) { json =>
      HttpEntity(ContentTypes.`application/json`, ByteString(printer.prettyByteBuffer(json)))
    }
  }

  implicit def emptyMarshaller: ToEntityMarshaller[Unit] =
    Marshaller.withFixedContentType(ContentTypes.NoContentType) { _ => HttpEntity.Empty}

  implicit def marshaller[A: Encoder](implicit printer: Printer = Printer.noSpaces): ToEntityMarshaller[A] =
    jsonMarshaller(printer).compose(Encoder[A].apply)
}