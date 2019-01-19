package domain

import util.RichRegex.RichRegex

import scala.util.Try

case class Payment(cardNumber: String,
                   expirationDate: String,
                   CVC: String,
                   money: String,
                   comment: Option[String],
                   email: String) {
  require("[0-9]{16}".r matches cardNumber)
  require("([0-9]|0[0-9]|1[0-2])\\/[1-9][0-9]".r matches expirationDate)
  require("[0-9]{3}" matches CVC)
  require(Try(money.toInt).map(m => m >= 1000 && m <= 75000).getOrElse(false))
  require(comment.forall(_.length <= 150))
  require("(?=[^\\s]+)(?=(\\w+)@([\\w\\.]+))".r matches email)
}
