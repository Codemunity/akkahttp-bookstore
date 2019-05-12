package repositories

import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

import helpers.BookSpecHelper
import models._
import org.scalatest.{AsyncWordSpec, BeforeAndAfter, MustMatchers}
import services.{ConfigService, FlywayService, PostgresService}

class OrderRepositorySpec
    extends AsyncWordSpec
    with MustMatchers
    with BeforeAndAfter
    with ConfigService {

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(jdbcUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)
  val bookRepository = new BookRepository(databaseService)
  val userRepository = new UserRepository(databaseService)
  val orderRepository = new OrderRepository(databaseService)

  val bookSpecHelper = new BookSpecHelper(categoryRepository)(bookRepository)

  def user = User(None, "Name", UUID.randomUUID().toString, "password")

  before {
    // Let's make sure our schema is created
    flywayService.migrateDatabase
  }

  after {
    // Let's make sure our schema is dropped
    flywayService.dropDatabase
  }

  "An OrderRepository" must {

    "create orders" in {
      for {
        u <- userRepository.create(user)
        books <- bookSpecHelper.bulkInsert
        order = Order(None, Timestamp.from(Instant.now()), u.id.get, 10.0)
        orderWithBooks = OrderWithBooks(order, books)
        co <-  orderRepository.createOrder(orderWithBooks)
      } yield {
        co.id mustBe defined
      }
    }

    "find orders by user" in {
      for {
        u <- userRepository.create(user)
        books <-  bookSpecHelper.bulkInsert
        order = Order(None, Timestamp.from(Instant.now()), u.id.get, 10.0)
        orderWithBooks = OrderWithBooks(order, books)
        co <-  orderRepository.createOrder(orderWithBooks)
        orders <- orderRepository.findOrdersByUser(u.id.get)
      } yield {
        orders.size mustBe 1
        orders.head.id mustBe co.id
      }
    }

    "find books by order" in {
      for {
        u <- userRepository.create(user)
        books <- bookSpecHelper.bulkInsert
        order = Order(None, Timestamp.from(Instant.now()), u.id.get, 10.0)
        orderWithBooks = OrderWithBooks(order, books)
        co <-  orderRepository.createOrder(orderWithBooks)
        storedBooks <- orderRepository.findBooksByOrder(co.id.get)
      } yield {
        storedBooks.size mustBe books.size
      }
    }

    "filters properly" in {
      for {
        u <- userRepository.create(user)
        books <- bookSpecHelper.bulkInsert
        order = Order(None, Timestamp.from(Instant.now()), u.id.get, 10.0)
        orderWithBooks = OrderWithBooks(order, books)
        _ <-  orderRepository.createOrder(orderWithBooks)
        orders <- orderRepository.findOrdersByUser(-5)
        storedBooks <- orderRepository.findBooksByOrder(-5)
      } yield {
        orders mustBe empty
        storedBooks mustBe empty
      }
    }
  }
}
