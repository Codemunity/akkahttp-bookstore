package directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import models.User
import services.TokenService

import scala.concurrent.ExecutionContext

trait VerifyToken {

  // Our directive depends on a TokenService, let's make sure our implement
  val tokenService: TokenService

  // We also require an execution context for our futures
  implicit val ec: ExecutionContext

  def verifyToken: Directive1[User] = {
    // Let's fetch the token from the request headers
    headerValueByName("Authorization").flatMap { token =>
      // Now we fetch the user from the token
      onSuccess(tokenService.fetchUser(token)) flatMap {
        // If the member has a value, we simply `provide` it to the next directive
        case Some(user) => provide(user)
        // If the member does not exist, we terminate the directives flow with an `Unauthorized` status code
        case None => complete(StatusCodes.Unauthorized)
      }
    }
  }

  // We use `flatMap` because we want to transform the directive for composition, instead of applying them
  def verifyTokenUser(userId: Long): Directive1[User] = verifyToken flatMap { userInToken =>
    userInToken.id match {
      case Some(id) =>
        // If the user in the token is the same as the user in our database,
        // we provide it a continue with the rest of the directives
        if (userId == id) provide(userInToken)
        // Else we terminate the directives flow with an `Unauthorized` status code
        else complete(StatusCodes.Unauthorized)
      case _ => complete(StatusCodes.Unauthorized)
    }
  }

}