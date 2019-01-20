package domain

import domain.VATOptions.VATOptions
import io.circe.generic.JsonCodec
import util.RichRegex.RichRegex

import scala.util.Try

@JsonCodec(decodeOnly = true) case class Request(taxId: String, BIC: String, accountNumber: String,
              VAT: VATOptions, money: String, telephone: String, email: String) {
  require("([0-9]{10}|[0-9]{12})".r matches taxId)
  require("[0-9]{9}".r matches BIC)
  require("[0-9]{20}".r matches accountNumber)
  require(Try(money.toInt).map(m => m >= 1000 && m <= 75000).getOrElse(false))
  require("+7[0-9]{10}".r matches telephone)
  require("(?=[^\\s]+)(?=(\\w+)@([\\w\\.]+))".r matches email)
}
