package domain

case class DBPayment(id: Int, cardNumber: String, expirationMonth: Int, expirationYear: Int,
                     CVC: String, money: String, comment: String, email: String, isSafe: Boolean)