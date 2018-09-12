import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.MissingHeaderRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import controllers.BookController
import helpers.BookSpecHelper
import models.{Book, BookJson, BookSearch, User}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repositories.{BookRepository, CategoryRepository, UserRepository}
import services.{ConfigService, FlywayService, PostgresService, TokenService}


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

  val userRepository = new UserRepository(databaseService)
  val tokenService = new TokenService(userRepository)
  val bookController = new BookController(bookRepository, tokenService)

  // SETUP AND TEAR DOWN

  override def beforeAll {
    // Let's make sure our schema is created
    flywayService.migrateDatabase

    bookSpecHelper.bulkInsert
  }

  override def afterAll {
    bookSpecHelper.bulkDelete
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

    "reject the request when there is no token in the request" in {
      Get("/books/123123") ~> bookController.routes ~> check {
        // We assert that the rejection should be about the missing header "Authorization"
        rejection mustBe MissingHeaderRejection("Authorization")
      }
    }

    "return `Unauthorized` when there is an invalid token in the request" in {
      // A sample user
      val invalidUser = User(Some(123123), "Name", "Email", "password")
      // Create the token without persisting the user
      val invalidToken = tokenService.createToken(invalidUser)

      // Add the Authorization header to the request
      Get("/books/123123") ~> addHeader("Authorization", invalidToken) ~> bookController.routes ~> check {
        // Since the token is invalid, we expect it to be completed with the "Unauthorized
        status mustBe StatusCodes.Unauthorized
      }
    }

    "return the book information when the token is valid" in {
      def assertion(token: String, bookId: Long) = {
        Get(s"/books/$bookId") ~> addHeader("Authorization", token) ~> bookController.routes ~> check {

          // Parse the response as a book
          val book = responseAs[Book]
          // Assert we receive the expected book
          book.title mustBe "Akka in Action"
          book.author mustBe "Raymond Roestenburg, Rob Bakker, and Rob Williams"
        }
      }

      // Sample user
      val user = User(None, "Name", "test@test.com", "password")
      // Our search, to get one book
      val bookSearch = BookSearch(Some("Akka in Action"))

      for {
        // Store the sample user
        storedUser <- userRepository.create(user)
        // Search for the expected book
        books <- bookRepository.search(bookSearch)
        // Perform the assertion using the token with a stored user and the expected book
        result <- assertion(tokenService.createToken(storedUser), books.head.id.get)
        // Delete the stored user for clean up
        _ <- userRepository.delete(storedUser.id.get)
      } yield result
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
