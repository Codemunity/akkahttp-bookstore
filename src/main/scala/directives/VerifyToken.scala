package directives

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.Directives._
import models.User
import services.TokenService

import scala.concurrent.{ExecutionContext}

trait VerifyToken {

  // Our directive depends on a TokenService, let's make sure our implement
  val tokenService: TokenService

  def verifyToken(implicit ec: ExecutionContext): Directive1[User] = {
    // Let's fetch the token from the request headers
    headerValueByName("Authorization").flatMap { token =>
      // Now we fetch the user from the token
      onSuccess(tokenService.fetchUser(token)) flatMap {
        // If the member has a value, we simply `provide` it to the next directive
        case Some(member) => provide(member)
        // If the member does not exist, we terminate the directives flow with an `Unauthorized` status code
        case None => complete(StatusCodes.Unauthorized)
      }
    }
  }

}