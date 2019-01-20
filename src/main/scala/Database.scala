import doobie.Transactor
import domain._
import cats.effect.IO
import doobie.implicits._
import domain.Payment

import scala.concurrent.ExecutionContext

class Database(config: DatabaseConfig)(implicit executionContext: ExecutionContext) {
  implicit val cs = IO.contextShift(executionContext)
  val xa = Transactor.fromDriverManager[IO](
    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
    config.url,
    config.user,
    config.password)

  def addPayment(payment: Payment): IO[Int] = {
    sql"""insert into payments values ${payment.cardNumber}, ${payment.expirationMonth}, ${payment.expirationYear},
         |${payment.CVC}, ${payment.money.toInt}, ${payment.comment.getOrElse("null")}, ${payment.email};"""
      .stripMargin.update.run.transact(xa)
  }

  def addRequest(request: Request): IO[Int] = {
    sql"""insert into requests values ${request.taxId}, ${request.BIC}, ${request.accountNumber},
         |${request.VAT.toString}, ${request.money.toInt}, ${request.telephone}, ${request.email};"""
      .stripMargin.update.run.transact(xa)
  }
}
