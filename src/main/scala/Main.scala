import scala.util.{Failure, Success}

object Main extends App {
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