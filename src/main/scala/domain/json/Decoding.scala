package domain.json

import domain.SortingOptions.SortingOptions
import domain._
import domain.VATOptions.VATOptions
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object Decoding {
  implicit val VATDecoder: Decoder[VATOptions] = Decoder.enumDecoder(VATOptions)
  implicit val paymentDecoder: Decoder[Payment] = deriveDecoder[Payment]
  implicit val internetBankPaymentDecoder: Decoder[InternetBankPayment] = deriveDecoder[InternetBankPayment]
  implicit val requestDecoder: Decoder[Request] =  deriveDecoder[Request]
  implicit val sortingOptionsDecoder: Decoder[SortingOptions] = Decoder.enumDecoder(SortingOptions)
  implicit val rangeRequestDecoder: Decoder[RangeRequest] = deriveDecoder[RangeRequest]
  implicit val setSafetyRequestDecoder: Decoder[SetSafetyRequest] = deriveDecoder[SetSafetyRequest]
}
