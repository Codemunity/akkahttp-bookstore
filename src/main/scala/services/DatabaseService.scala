package services

import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._


trait DatabaseService {

  val driver: JdbcProfile
  val db: Database

}