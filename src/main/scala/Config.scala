case class Config(database: DatabaseConfig)

case class DatabaseConfig(url: String, user: String, password: String)
