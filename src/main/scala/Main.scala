import akka.actor.ActorSystem
import scala.util.{Failure, Success}
import pureconfig.generic.auto._

import scala.concurrent.ExecutionContextExecutor

object Main extends App {
  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  pureconfig.loadConfig[Config] match {
    case Left(_) =>
      println("Failed to load config.")
      System.exit(1)
    case Right(config) =>
      new Server(config).start().onComplete({
        case Success(_) =>
          println("Server is up.")
        case Failure(_) =>
          println("Failed to start server.")
          System.exit(1)
      })
  }
}