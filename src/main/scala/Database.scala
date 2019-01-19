import akka.stream.actor.ActorPublisherMessage.Request
import doobie.Transactor
import cats.effect.IO
import domain.Payment

class Database(config: DatabaseConfig) {
  val xa = Transactor.fromDriverManager[IO](
    "com.microsoft.sqlserver.jdbc.SQLServerDriver",
    config.url,
    config.user,
    config.password)

  def addPayment(payment: Payment): Unit = {

  }

  def addRequest(request: Request): Unit = {

  }
}
