import doobie.Transactor
import doobie.implicits._
import domain._
import cats.effect.IO
import domain.Payment

class Database(config: DatabaseConfig) {
  val xa = Transactor.fromDriverManager[IO](
    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
    config.url,
    config.user,
    config.password)

  def addPayment(payment: Payment): IO[Int] = {
    sql"insert into payments values ${payment.cardNumber}, ${payment.expirationDate}, ${payment.CVC}, ${payment.money}, ${payment.comment.getOrElse("null")}, ${payment.email};"
      .update.run.transact(xa)
  }

  def addRequest(request: Request): IO[Int] = {
    sql"insert into requests values ${request.taxId}, ${request.BIC}, ${request.accountNumber}, ${request.VAT}, ${request.money}, ${request.telephone}, ${request.email};"
      .update.run.transact(xa)
  }
}
