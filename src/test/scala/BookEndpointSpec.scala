import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import controllers.BookController
import helpers.BookSpecHelper
import models.{Book, BookJson}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repository.{BookRepository, CategoryRepository}
import services.{ConfigService, FlywayService, PostgresService}


class BookEndpointSpec extends AsyncWordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with BookJson {

  // DEPENDENCIES

  override implicit val executor = system.dispatcher

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(jdbcUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)

  val bookRepository = new BookRepository(databaseService)

  val bookSpecHelper = new BookSpecHelper(categoryRepository)(bookRepository)

  val bookController = new BookController(bookRepository)

  // SETUP AND TEAR DOWN

  override def beforeAll {
    // Let's make sure our schema is created
    flywayService.migrateDatabase

    bookSpecHelper.bulkInsert
  }

  override def afterAll {
    bookSpecHelper.bulkDelete.map { _ =>
      categoryRepository.close
      bookRepository.close
    }

    // Let's make sure our schema is dropped
    flywayService.dropDatabase
  }

  "A Book Endpoint" must {

    "return all books when no query parameters are sent" in {
      Get("/books/") ~> bookController.routes ~> check {
        status mustBe StatusCodes.OK

        val books = responseAs[List[Book]]

        books must have size bookSpecHelper.bookFields.size
      }
    }

    "return all books that conform to the query parameters sent" in {
      Get("/books?title=in&author=Ray") ~> bookController.routes ~> check {
        status mustBe StatusCodes.OK

        val books = responseAs[List[Book]]

        books must have size 1
      }

      Get("/books?title=The") ~> bookController.routes ~> check {
        status mustBe StatusCodes.OK

        val books = responseAs[List[Book]]

        books must have size 2
      }
    }

    "create a book" in {
      categoryRepository.create(bookSpecHelper.category) flatMap { c =>
        Post("/books/", bookSpecHelper.book(c.id.get)) ~> bookController.routes ~> check {
          status mustBe StatusCodes.Created

          val book = responseAs[Book]

          // Let's delete the book and it's category to make sure we don't corrupt the rest of the tests

          for {
            _ <- bookRepository.delete(book.id.get)
            _ <- categoryRepository.delete(c.id.get)
          } yield {
            book.id mustBe defined
            book.title mustBe "Murder in Ganymede"
          }
        }
      }
    }

    "return NotFound when we try to delete a non existent category" in {
      Delete("/books/10/") ~> bookController.routes ~> check {
        status mustBe StatusCodes.NotFound
      }
    }

    "return NoContent when we delete an existent category" in {
      categoryRepository.create(bookSpecHelper.category) flatMap { c =>
        bookRepository.create(bookSpecHelper.book(c.id.get)) flatMap { b =>
          Delete(s"/books/${b.id.get}/") ~> bookController.routes ~> check {

            // Let's delete the category to make sure we don't corrupt the rest of the tests
            categoryRepository.delete(c.id.get)

            status mustBe StatusCodes.NoContent
          }
        }
      }
    }

  }

}
