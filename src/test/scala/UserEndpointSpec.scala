import scala.concurrent.Future

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import controllers.UserController
import models.{User, UserJson}
import org.scalatest.{Assertion, AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repositories.UserRepository
import services.{ConfigService, FlywayService, PostgresService}

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

  val userController = new UserController(userRepository)

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

    "return NotFound when no user is found by id" in {
      Get("/users/10/") ~> userController.routes ~> check {
        status mustBe StatusCodes.NotFound
      }
    }

    "return the user data when it is found by id" in {
      def assert(user: User): Future[Assertion] = {
        Get(s"/users/${user.id.get}") ~> userController.routes ~> check {
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

  }
}
