package domain

case class DBRequest(taxId: String, BIC: String, accountNumber: String,
                     VAT: String, money: String, telephone: String, email: String)