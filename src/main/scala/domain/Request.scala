package domain

import util.RichRegex.RichRegex

class Request(accountNumber: String, telephone: String) {
  require("[0-9]{20}".r matches accountNumber)
  require("+7[0-9]{10}".r matches telephone)
}
