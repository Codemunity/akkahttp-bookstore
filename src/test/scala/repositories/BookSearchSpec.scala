package repositories

import java.sql.Date

import helpers.BookSpecHelper
import models.{BookSearch, Category}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repository.{BookRepository, CategoryRepository}
import services.{ConfigService, FlywayService, PostgresService}
import scala.concurrent.Future


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

  val sciFiCategory = Category(None, "Sci-Fi")
  val techCategory = Category(None, "Technical")

  val bookFields = List(
    ("Akka in Action", techCategory.title, Date.valueOf("2016-09-01"), "Raymond Roestenburg, Rob Bakker, and Rob Williams"),
    ("Scala in Depth", techCategory.title, Date.valueOf("2012-01-01"), "Joshua D. Suereth"),
    ("Code Complete", techCategory.title, Date.valueOf("1993-01-01"), "Steve McConnell"),
    ("The Time Machine", sciFiCategory.title, Date.valueOf("1895-01-01"), "H.G. Wells"),
    ("The Invisible Man", sciFiCategory.title, Date.valueOf("1897-01-01"), "H.G. Wells"),
    ("Nineteen Eighty-Four", sciFiCategory.title, Date.valueOf("1949-01-01"), "George Orwell")
  )

  override def beforeAll {
    // Let's make sure our schema is created
    flywayService.migrateDatabase

    for {
      s <- categoryRepository.create(sciFiCategory)
      t <- categoryRepository.create(techCategory)
      b <- Future.sequence(bookFields.map { bookField =>
        // Get the respective category id
        val cId = if (bookField._2 == sciFiCategory.title) s.id.get else t.id.get
        val book = bookSpecHelper.book(cId, bookField._1, bookField._3, bookField._4)
        bookRepository.create(book)
      })
    } yield b
  }

  override def afterAll {
    for {
      books <- bookRepository.all
      _ <- Future.sequence(books.map(b => bookRepository.delete(b.id.get)))
      _ <- categoryRepository.delete(sciFiCategory.id.get)
      _ <- categoryRepository.delete(techCategory.id.get)
    } yield books

    // Let's make sure our schema is dropped
    flywayService.dropDatabase
  }

  "Performing a BookSearch" must {

    "return an empty list if there are no matches" in {
      val bookSearch = BookSearch(title = Some("Non existent book"))
      bookRepository.search(bookSearch).map { books =>
        books.size mustBe 0
      }
    }

    "return the matching books by title" in {
      val bookSearch = BookSearch(title = Some("Akka"))
      bookRepository.search(bookSearch).map { books =>
        books.size mustBe 1
        books.head.title mustBe bookFields.head._1
      }

      val bookSearchMultiple = BookSearch(title = Some("The"))
      bookRepository.search(bookSearchMultiple).map { books =>
        books.size mustBe 2
      }
    }

    "return the books by release date" in {
      val bookSearch = BookSearch(releaseDate = Some(Date.valueOf("1993-01-01")))
      bookRepository.search(bookSearch).map { books =>
        books.size mustBe 1
        books.head.title mustBe bookFields(2)._1
      }
    }

    "return the books by category" in {
      for {
        Some(category) <- categoryRepository.findByTitle(sciFiCategory.title)
        books <- bookRepository.search(BookSearch(categoryId = category.id))
      } yield books.size mustBe 3
    }

    "return the books by author" in {
      val bookSearch = BookSearch(author = Some(". We"))
      bookRepository.search(bookSearch).map { books =>
        books.size mustBe 2
      }
    }

  }

}