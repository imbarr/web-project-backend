import akka.actor.ActorSystem
import com.typesafe.scalalogging.Logger

import scala.util.{Failure, Success}
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object Main extends App {
  implicit val log = Logger("spa-backend")
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  pureconfig.loadConfig[Config] match {
    case Left(_) =>
      log.error("Fatal: failed to load configuration.")
      System.exit(1)
    case Right(config) =>
      new Server(config).start().onComplete({
        case Success(_) =>
          log.info("Server is ready.")
        case Failure(_) =>
          log.error("Fatal: server failed to start.")
          System.exit(1)
      })
  }
}