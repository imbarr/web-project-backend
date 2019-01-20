package domain.json

import domain.{InternetBankPayment, Payment, Request, VATOptions}
import domain.VATOptions.VATOptions
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object Decoding {
  implicit val VATDecoder: Decoder[VATOptions] = Decoder.enumDecoder(VATOptions)
  implicit val paymentDecoder: Decoder[Payment] = deriveDecoder[Payment]
  implicit val internetBankPaymentDecoder: Decoder[InternetBankPayment] = deriveDecoder[InternetBankPayment]
  implicit val requestDecoder: Decoder[Request]=  deriveDecoder[Request]
}
