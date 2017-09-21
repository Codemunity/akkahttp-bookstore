package repositories

import java.sql.Date

import helpers.BookSpecHelper
import models.{BookSearch, Category}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repository.{BookRepository, CategoryRepository}
import services.{ConfigService, FlywayService, PostgresService}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class BookSearchSpec extends AsyncWordSpec
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

  "Performing a BookSearch" must {

    "return an empty list if there are no matches" in {
      bookSpecHelper.bulkInsertAndDelete { books =>
        val bookSearch = BookSearch(title = Some("Non existent book"))
        bookRepository.search(bookSearch).map { books =>
          books.size mustBe 0
        }
      }
    }

    "return the matching books by title" in {
      bookSpecHelper.bulkInsertAndDelete { books =>
        val bookSearch = BookSearch(title = Some("Akka"))
        bookRepository.search(bookSearch).map { books =>
          books.size mustBe 1
          books.head.title mustBe bookSpecHelper.bookFields.head._1
        }

        val bookSearchMultiple = BookSearch(title = Some("The"))
        bookRepository.search(bookSearchMultiple).map { books =>
          books.size mustBe 2
        }
      }
    }

    "return the books by release date" in {
      bookSpecHelper.bulkInsertAndDelete { books =>
        val bookSearch = BookSearch(releaseDate = Some(Date.valueOf("1993-01-01")))
        bookRepository.search(bookSearch).map { books =>
          books.size mustBe 1
          books.head.title mustBe bookSpecHelper.bookFields(2)._1
        }
      }
    }

    "return the books by category" in {
      bookSpecHelper.bulkInsertAndDelete { books =>
        for {
          Some(category) <- categoryRepository.findByTitle(bookSpecHelper.sciFiCategory.title)
          books <- bookRepository.search(BookSearch(categoryId = category.id))
        } yield books.size mustBe 3
      }
    }

    "return the books by author" in {
      bookSpecHelper.bulkInsertAndDelete { books =>
        val bookSearch = BookSearch(author = Some(". We"))
        bookRepository.search(bookSearch).map { books =>
          books.size mustBe 2
        }
      }
    }

    "return correctly the expect books when combining searches" in {
      bookSpecHelper.bulkInsertAndDelete { books =>
        for {
          Some(category) <- categoryRepository.findByTitle(bookSpecHelper.sciFiCategory.title)
          books <- bookRepository.search(BookSearch(categoryId = category.id, title = Some("Scala")))
        } yield books.size mustBe 0

        val bookSearch = BookSearch(author = Some("H.G."), title = Some("The"))
        bookRepository.search(bookSearch).map { books =>
          books.size mustBe 2
        }
      }
    }

  }

}