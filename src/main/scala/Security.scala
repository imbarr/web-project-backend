import java.security.MessageDigest
import akka.http.scaladsl.server.directives.Credentials

import scala.concurrent.Future

class Security(db: Database) {
  private def getPasswordHash(password: String): String = {
    MessageDigest.getInstance("SHA-256")
      .digest(password.getBytes("UTF-8"))
      .map("%02X" format _).mkString
  }

  def authenticator(c: Credentials): Future[Option[String]] = c match {
    case p @ Credentials.Provided(username) =>
      db.getPasswordHash(username)
        .map(hash => p.verify(hash, getPasswordHash))
        .map(if(_) Some(username) else None)
        .unsafeToFuture()
    case _ => Future.successful(None)
  }
}
