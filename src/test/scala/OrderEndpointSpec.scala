import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import controllers.OrderController
import helpers.BookSpecHelper
import models._
import org.scalatest.{AsyncWordSpec, BeforeAndAfter, MustMatchers}
import repositories.{BookRepository, CategoryRepository, OrderRepository, UserRepository}
import services.{ConfigService, FlywayService, PostgresService, TokenService}

class OrderEndpointSpec extends AsyncWordSpec
  with MustMatchers
  with BeforeAndAfter
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with OrderJson {

  override implicit val executor = system.dispatcher

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(jdbcUrl, dbUser, dbPassword)

  val orderRepository = new OrderRepository(databaseService)
  val userRepository = new UserRepository(databaseService)
  val categoryRepository = new CategoryRepository(databaseService)
  val bookRepository = new BookRepository(databaseService)

  val tokenService = new TokenService(userRepository)
  val bookSpecHelper = new BookSpecHelper(categoryRepository)(bookRepository)

  val orderController = new OrderController(orderRepository, tokenService)

  def user = User(None, "Name", UUID.randomUUID().toString, "password")

  before {
    // Let's make sure our schema is created
    flywayService.migrateDatabase
  }

  after {
    // Let's make sure our schema is dropped
    flywayService.dropDatabase
  }

  "An OrderEndpoint" must {
    "should create orders" in {
      for {
        u <- userRepository.create(user)
        token = tokenService.createToken(u)
        books <- bookSpecHelper.bulkInsert
        order = Order(None, Timestamp.from(Instant.now()), u.id.get, 10.0)
        orderWithBooks = OrderWithBooks(order, books)
      } yield Post("/orders", orderWithBooks) ~> addHeader("Authorization", token) ~> orderController.routes ~> check {
        status mustBe StatusCodes.Created
      }
    }

    "find orders by user" in {
      for {
        u <- userRepository.create(user)
        token = tokenService.createToken(u)
        books <- bookSpecHelper.bulkInsert
        order = Order(None, Timestamp.from(Instant.now()), u.id.get, 10.0)
        orderWithBooks = OrderWithBooks(order, books)
        co <- orderRepository.createOrder(orderWithBooks)
      } yield Get(s"/orders") ~> addHeader("Authorization", token) ~> orderController.routes ~> check {
        status mustBe StatusCodes.OK
        val orders = responseAs[Seq[Order]]
        orders mustBe Seq(co)
      }
    }

    "find books by order" in {
      for {
        u <- userRepository.create(user)
        token = tokenService.createToken(u)
        books <- bookSpecHelper.bulkInsert
        order = Order(None, Timestamp.from(Instant.now()), u.id.get, 10.0)
        orderWithBooks = OrderWithBooks(order, books)
        co <- orderRepository.createOrder(orderWithBooks)
      } yield Get(s"/orders/${co.id.get}/books") ~> addHeader("Authorization", token) ~> orderController.routes ~> check {
        status mustBe StatusCodes.OK
        val booksByOrder = responseAs[Seq[BookByOrder]]
        booksByOrder.size mustBe books.size
      }
    }
  }
}
