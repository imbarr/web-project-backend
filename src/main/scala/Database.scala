import java.security.MessageDigest

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
    fr"""insert into payments (cardNumber, expirationMonth, expirationYear, CVC, money, comment, email)
         |values (${payment.cardNumber}, ${payment.expirationMonth}, ${payment.expirationYear},
         |${payment.CVC}, ${payment.money.toInt}, ${payment.comment}, ${payment.email});""")

  def addRequest(request: Request): IO[Unit] = updateSingleValue(
    fr"""insert into requests (taxId, BIC, accountNumber, VAT, money, telephone, email)
         |values (${request.taxId}, ${request.BIC}, ${request.accountNumber},
         |${request.VAT.toString}, ${request.money.toInt}, ${request.telephone}, ${request.email});""")

  def updateSingleValue(query: Fragment): IO[Unit] = {
    query.stripMargin.update.run.transact(xa).map(
      i =>
        if(i == 1) Unit
        else throw new Exception("No data modified")
    )
  }

  def getPaymentRange(rangeRequest: RangeRequest): IO[List[DBPayment]] = {
    if(!List("id", "cardNumber", "expirationMonth", "expirationYear", "CVC", "money",
      "comment", "email", "isSafe").contains(rangeRequest.sortField))
      throw new Exception("Table payments does not have column " + rangeRequest.sortField)

    (fr"""select * from payments where id between ${rangeRequest.start} and ${rangeRequest.end} order by""" ++
      Fragment.const(rangeRequest.sortField) ++ Fragment.const(rangeRequest.order.toString))
      .stripMargin.query[DBPayment].to[List].transact(xa)
  }

  def getPaymentRowNumber: IO[Int] = {
    fr"select count(*) from payments".query[Int].unique.transact(xa)
  }

  def getRequestRowNumber: IO[Int] = {
    fr"select count(*) from requests".query[Int].unique.transact(xa)
  }

  def getRequestRange(rangeRequest: RangeRequest): IO[List[DBRequest]] = {
    if(!List("id", "taxId", "BIC", "accountNumber", "VAT", "money", "telephone", "email")
      .contains(rangeRequest.sortField))
      throw new Exception("Table requests does not have column " + rangeRequest.sortField)

    (fr"""select * from requests where id between ${rangeRequest.start} and ${rangeRequest.end} order by""" ++
      Fragment.const(rangeRequest.sortField) ++ Fragment.const(rangeRequest.order.toString))
      .stripMargin.query[DBRequest].to[List].transact(xa)
  }

  def changeSafety(safetyRequest: SetSafetyRequest): IO[Seq[Int]] = {
    val safe = if(safetyRequest.isSafe) 1 else 0
    val list = fr"(" ++ safetyRequest.id.dropRight(1).map(i => fr"$i, ").fold(fr"")(_ ++ _) ++
      fr"${safetyRequest.id.last})"
    (fr"""update payments set isSafe = $safe where id in""" ++ list ++ fr";")
      .update.run.transact(xa).map(_ => safetyRequest.id)
  }

  def getPasswordHash(login: String): IO[String] = {
    fr"select password from admins where login = $login".query[String].unique.transact(xa)
  }
}