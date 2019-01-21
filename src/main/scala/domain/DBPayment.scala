package domain

case class DBPayment(cardNumber: String, expirationDate: String, CVC: String, money: String,
                     comment: String, email: String)