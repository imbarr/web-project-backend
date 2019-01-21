package domain

case class DBRequest(id: Int, taxId: String, BIC: String, accountNumber: String,
                     VAT: String, money: String, telephone: String, email: String)