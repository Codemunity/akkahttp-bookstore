import scala.concurrent.Future

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import controllers.UserController
import models.{User, UserJson}
import org.scalatest.{Assertion, AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repository.UserRepository
import services.{ConfigService, FlywayService, PostgresService, TokenService}

class UserEndpointSpec extends AsyncWordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with UserJson
{

  override implicit val executor = system.dispatcher

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(jdbcUrl, dbUser, dbPassword)

  val userRepository = new UserRepository(databaseService)

  val tokenService = new TokenService(userRepository)
  val userController = new UserController(userRepository, tokenService)

  val user = User(None, "Name", "email", "password")

  override def beforeAll {
    // Let's make sure our schema is created
    flywayService.migrateDatabase
  }

  override def afterAll {
    // Let's make sure our schema is dropped
    flywayService.dropDatabase
  }

  "A UserEndpoint" must {

    "return BadRequest with repeated emails" in {
      def assert(user: User): Future[Assertion] = {
        Post("/users", user) ~> userController.routes ~> check {
          status mustBe StatusCodes.BadRequest
        }
      }

      for {
        u <- userRepository.create(user)
        result <- assert(user)
        _ <- userRepository.delete(u.id.get)
      } yield result
    }

    "create a user" in {

      def assert(user: User): Future[Assertion] = {
        Post("/users", user) ~> userController.routes ~> check {
          status mustBe StatusCodes.Created

          val createdUser = responseAs[User]
          createdUser.email mustBe user.email
        }
      }

      for {
        result <- assert(user)
        Some(u) <- userRepository.findByEmail(user.email)
        _ <- userRepository.delete(u.id.get)
      } yield result
    }

    "return Unauthorized when no user is found by id" in {
      // A sample user
      val invalidUser = User(Some(123123), "Name", "Email", "password")
      // Create the token without persisting the user
      val invalidToken = tokenService.createToken(invalidUser)

      Get("/users/10/") ~> addHeader("Authorization", invalidToken) ~> userController.routes ~> check {
        status mustBe StatusCodes.Unauthorized
      }
    }

    "return the user data when it is found by id" in {
      def assert(user: User): Future[Assertion] = {
        // Create a token with the same user as the one requested
        val token = tokenService.createToken(user)

        Get(s"/users/${user.id.get}") ~> addHeader("Authorization", token) ~> userController.routes ~> check {
          status mustBe StatusCodes.OK

          val foundUser = responseAs[User]
          foundUser.email mustBe user.email
        }
      }

      for {
        u <- userRepository.create(user)
        result <- assert(u)
        _ <- userRepository.delete(u.id.get)
      } yield result
    }

    "return Unauthorized when the user requested is not the same in the token" in {
      def assert(user: User, token: String): Future[Assertion] = {

        Get(s"/users/${user.id.get}") ~> addHeader("Authorization", token) ~> userController.routes ~> check {
          status mustBe StatusCodes.Unauthorized
        }
      }

      val user2 = User(None, "Name2", "email2", "password2")

      for {
        u <- userRepository.create(user)
        u2 <- userRepository.create(user2)
        // Create a token with our second user
        result <- assert(u, tokenService.createToken(u2))
        _ <- userRepository.delete(u.id.get)
        _ <- userRepository.delete(u2.id.get)
      } yield result
    }

  }
}
