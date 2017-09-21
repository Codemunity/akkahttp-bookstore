package repositories

import helpers.BookSpecHelper
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repository.{BookRepository, CategoryRepository}
import services.{ConfigService, FlywayService, PostgresService}


class BookRepositorySpec extends AsyncWordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with ConfigService {

  /*
    Instantiate our service in charge of migrating our schema.
    We need to make sure it exists before all our tests are ran,
    and we also need to make sure our schema is destroyed after all tests are ran,
     because we need to always start from a clean slate.
   */
  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

  // We need a service that provides us access to our database
  val databaseService = new PostgresService(jdbcUrl, dbUser, dbPassword)

  // We need access to our "categories" table, to provide our books with their dependencies
  val categoryRepository = new CategoryRepository(databaseService)

  // We need access to our "books" table
  val bookRepository = new BookRepository(databaseService)

  // Our class for book-related helper methods
  val bookSpecHelper = new BookSpecHelper(categoryRepository)(bookRepository)

  override def beforeAll {
    // Let's make sure our schema is created
    flywayService.migrateDatabase
  }

  override def afterAll {

    // Let's make sure our schema is dropped
    flywayService.dropDatabase
  }


  "A BookRepository" must {

    "be empty at the beginning" in {
      bookRepository.all map { bs => bs.size mustBe 0 }
    }

    "create valid books" in {

      bookSpecHelper.createAndDelete() { b =>
        b.id mustBe defined
        bookRepository.all map { cs => cs.size mustBe 1 }
      }
    }

    "not find a non-existent book" in {
      bookRepository.findById(0) flatMap { book =>
        book must not be defined
      }
    }

    "find an existing book" in {

      bookSpecHelper.createAndDelete() { b =>
        bookRepository.findById(b.id.get) flatMap { book =>
          book mustBe defined
          book.get.title mustEqual b.title
        }
      }
    }

    "delete a book by id if it exists" in {

      for {
        category <- categoryRepository.create(bookSpecHelper.category)
        book <- bookRepository.create(bookSpecHelper.book(category.id.get))
        _  <- bookRepository.delete(book.id.get)
        books <- bookRepository.all
        _ <- categoryRepository.delete(category.id.get)
      } yield books.size mustBe 0
    }

  }
}
