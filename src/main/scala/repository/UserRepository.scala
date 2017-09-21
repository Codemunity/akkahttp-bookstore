package repository

import models._
import services.DatabaseService

import scala.concurrent.{ExecutionContext, Future}
import com.github.t3hnar.bcrypt._


class UserRepository(val databaseService: DatabaseService)(implicit executor: ExecutionContext) extends UserTable {
  import databaseService._
  import databaseService.driver.api._

  // Here we just return the whole table query
  def all: Future[Seq[User]] = db.run(users.result)

  // We add the user to our existing table query
  def create(user: User): Future[User] = {
    val secureUser = user.copy(password =  user.password.bcrypt)
    db.run(users returning users += secureUser)
  }

  // Here we just filter our table query by id
  def findById(id: Long): Future[Option[User]] = db.run(users.filter(_.id === id).result.headOption)

  // Here we just filter our table query by email
  def findByEmail(email: String): Future[Option[User]] = db.run(users.filter(_.email === email).result.headOption)

  // Here we find the respective category and then we delete it
  def delete(id: Long): Future[Int] = db.run(users.filter(_.id === id).delete)
}