package services

import com.typesafe.config.ConfigFactory


trait ConfigService {

  // Load the default config file
  private val config = ConfigFactory.load()
  // Load the "http" config object
  private val httpConfig = config.getConfig("http")
  // Load the "database" config object
  private val databaseConfig = config.getConfig("database")

  // Grab the "interface" parameter from the http config
  val httpHost = httpConfig.getString("interface")
  // Grab the "port" parameter from the http config
  val httpPort = httpConfig.getInt("port")

  // Grab the "url" parameter from the database config
  val jdbcUrl = databaseConfig.getString("url")
  // Grab the "user" parameter from the database config
  val dbUser = databaseConfig.getString("user")
  // Grab the "password" parameter from the database config
  val dbPassword = databaseConfig.getString("password")

}
