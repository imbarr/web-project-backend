package domain

import domain.VATOptions.VATOptions
import util.RichRegex.RichRegex

case class InternetBankPayment(taxId: String, BIC: String, VAT: VATOptions) {
  require("([0-9]{10}|[0-9]{12})".r matches taxId)
  require("[0-9]{9}".r matches BIC)
}
