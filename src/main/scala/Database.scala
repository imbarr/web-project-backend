import doobie.Transactor
import domain._
import cats.effect.IO
import doobie.implicits._
import domain.Payment
import doobie.util.fragment.Fragment

import scala.concurrent.ExecutionContext

class Database(config: DatabaseConfig)(implicit executionContext: ExecutionContext) {
  implicit val cs = IO.contextShift(executionContext)
  val xa = Transactor.fromDriverManager[IO](
    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
    config.url,
    config.user,
    config.password)

  def addPayment(payment: Payment): IO[Unit] = updateSingleValue(
    sql"""insert into payments (card_number, expiration_month, expiration_year, cvc, money, comment, email)
         |values (${payment.cardNumber}, ${payment.expirationMonth}, ${payment.expirationYear},
         |${payment.CVC}, ${payment.money.toInt}, ${payment.comment}, ${payment.email});"""
  )

  def addRequest(request: Request): IO[Unit] = updateSingleValue(
    sql"""insert into requests (tax_id, bic, account_number, vat, money, telephone, email)
         |values (${request.taxId}, ${request.BIC}, ${request.accountNumber},
         |${request.VAT.toString}, ${request.money.toInt}, ${request.telephone}, ${request.email});"""
  )

  def updateSingleValue(query: Fragment): IO[Unit] = {
    query.stripMargin.update.run.transact(xa).map(
      i =>
        if(i == 1) Unit
        else throw new Exception("No data modified")
    )
  }

  def getPaymentRange(rangeRequest: RangeRequest): IO[List[DBPayment]] = {
    getRange("payments", rangeRequest).query[DBPayment].to[List].transact(xa)
  }

  def getRequestRange(rangeRequest: RangeRequest): IO[List[DBRequest]] = {
    getRange("requests", rangeRequest).query[DBRequest].to[List].transact(xa)
  }

  def getRange(table: String, rangeRequest: RangeRequest): Fragment = {
    sql"""select * from $table where id between ${rangeRequest.start} and ${rangeRequest.end}
         |order by ${rangeRequest.sortField} ${rangeRequest.order.toString};"""
  }

  def changeSafety(safetyRequest: SetSafetyRequest): IO[Unit] = {
    val safe = if(safetyRequest.isSafe) 1 else 0
    updateSingleValue(sql"""update payments set isSafe = $safe where id = ${safetyRequest.id}""")
  }
}