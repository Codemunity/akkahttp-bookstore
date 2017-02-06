package repository

import models._
import services.DatabaseService

import scala.concurrent.{ExecutionContext, Future}


class UserRepository(val databaseService: DatabaseService)(implicit executor: ExecutionContext) extends UserTable {
  import databaseService._
  import databaseService.driver.api._

  // Here we just return the whole table query
  def all: Future[Seq[User]] = db.run(users.result)

  // We add the user to our existing table query
  def create(user: User): Future[User] = db.run(users returning users += user)

  // Here we just filter our table query by title
  def findById(id: Long): Future[Option[User]] = db.run(users.filter(_.id === id).result.headOption)

  // Here we find the respective category and then we delete it
  def delete(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)
}