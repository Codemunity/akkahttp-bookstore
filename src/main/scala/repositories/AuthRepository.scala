package repositories

import scala.concurrent.{ExecutionContext, Future}

import com.github.t3hnar.bcrypt._
import models._
import services.DatabaseService


class AuthRepository(val databaseService: DatabaseService)(implicit executor: ExecutionContext) extends UserTable {
  import databaseService._
  import databaseService.driver.api._

  def findByCredentials(credentials: Credentials): Future[Option[User]] =
  // We first look for for a user with the email in our `Credentials`
    db.run(users.filter(_.email === credentials.email).result.headOption) map {
      // If a user is found, let's see if it matches the password in our `Credentials`
    case result @ Some(user) =>
      // If the user matches the password received, we return the `result`, which is a `Some(user)`
      if (credentials.password.isBcrypted(user.password)) result
      else None
    case None => None
  }
}
