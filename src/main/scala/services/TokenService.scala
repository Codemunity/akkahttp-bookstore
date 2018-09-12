package services

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

import models.{User, UserJson}
import pdi.jwt.{Jwt, JwtAlgorithm}
import repositories.UserRepository
import spray.json._

class TokenService(val userRepository: UserRepository)(implicit ec: ExecutionContext) extends UserJson {

  // Key used by the hashing library, you might want to fetch this from somewhere else, such as our Config service
  private val tempKey = "mySuperSecretAuthKey"

  // A method to create tokens based on our User data
  def createToken(user: User): String = {
    Jwt.encode(user.id.get.toJson.toString, tempKey, JwtAlgorithm.HS256)
  }

  // Convenience method that determines whether a token is valid
  def isTokenValid(token: String): Boolean = {
    Jwt.isValid(token, tempKey, Seq(JwtAlgorithm.HS256))
  }

  // Helper method to take the `id` from the token, and fetch the respective user from the database
  def fetchUser(token: String): Future[Option[User]] = {
    Jwt.decodeRaw(token, tempKey, Seq(JwtAlgorithm.HS256)) match {
      case Success(json) =>
        // Convert the JSON to the `id` that was encoded in our token
        val id = json.parseJson.convertTo[Long]
        // Try to find the user with the decoded `id`
        userRepository.findById(id)
      case Failure(e) => Future.failed(e)
    }
  }

  // Convenience method to determine whether a token is valid, and belongs to a certain user
  def isTokenValidForMember(token: String, user: User): Future[Boolean] = fetchUser(token).map {
    case Some(fetchedUser) => user.id == fetchedUser.id
    case None => false
  }
}
