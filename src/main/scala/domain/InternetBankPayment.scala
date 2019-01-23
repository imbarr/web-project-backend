package domain

import domain.VATOptions.VATOptions
import util.RichRegex.RichRegex

import scala.util.Try

object InternetBankPayment {
  def unapply(v: Map[String, String]) =
    Try(InternetBankPayment(
      v("taxId"), v("BIC"), v("accountNumber"), VATOptions.withName(v("VAT")), v("money"))).toOption
}

case class InternetBankPayment(taxId: String, BIC: String, accountNumber: String, VAT: VATOptions, money: String) {
  require("([0-9]{10}|[0-9]{12})".r matches taxId)
  require("[0-9]{9}".r matches BIC)
  require("[0-9]{20}".r matches accountNumber)
  require(Try(money.toInt).map(m => m >= 1000 && m <= 75000).getOrElse(false))
}
