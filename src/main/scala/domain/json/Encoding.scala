package domain.json

import domain.{DBPayment, DBRequest, RangeResponse}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

object Encoding {
  implicit val DBPaymentEncoder = deriveEncoder[DBPayment]
  implicit val DBRequestEncoder = deriveEncoder[DBRequest]
  implicit def rangeResponseEncoder[T: Encoder] = deriveEncoder[RangeResponse[T]]
}
