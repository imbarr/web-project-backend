package domain

import util.RichRegex.RichRegex

import scala.util.Try

case class Payment(cardNumber: String,
                   expirationDate: String,
                   CVC: String,
                   money: String,
                   comment: String,
                   email: String) {
  require("[0-9]{16}".r matches cardNumber)
  require("([0-9]|0[0-9]|1[0-2])\\/[1-9][0-9]".r matches expirationDate)
  require("[0-9]{3}".r matches CVC)
  require(Try(money.toInt).map(m => m >= 1000 && m <= 75000).getOrElse(false))
  require(comment.length <= 150)
  require("[^@]+@[^\\.]+\\..+".r matches email)

  val expirationMonth: Int = expirationDate.split("/")(0).toInt
  val expirationYear: Int = expirationDate.split("/")(1).toInt
}
