case class Config(database: DatabaseConfig, server: ServerConfig)

case class DatabaseConfig(url: String, user: String, password: String)

case class ServerConfig(interface: String, port: Int)
