package controllers

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import models.{Auth, AuthJson, Credentials, CredentialsJson}
import repositories.AuthRepository
import services.TokenService

class AuthController(val authRepository: AuthRepository, val tokenService: TokenService) extends CredentialsJson
  with AuthJson {

  val routes = pathPrefix("auth") {
    pathEndOrSingleSlash {
      post {
          decodeRequest {
            entity(as[Credentials]) { credentials =>
              onSuccess(authRepository.findByCredentials(credentials)) {
                case Some(user) => {
                  val token = tokenService.createToken(user)
                  complete(Auth(user, token))
                }
                case None => complete(StatusCodes.Unauthorized, "No user matched the credentials.")
              }
            }
          }
        }
    }
  }
}
