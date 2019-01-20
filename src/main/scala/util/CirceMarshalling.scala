package util
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.http.scaladsl.unmarshalling.{FromRequestUnmarshaller, Unmarshaller}
import akka.http.scaladsl.util.FastFuture
import akka.util.ByteString
import io.circe.{Decoder, Json, jawn}

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
}