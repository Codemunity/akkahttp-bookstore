package helpers

import java.sql.Date

import models.{Book, Category}
import org.scalatest._
import repository.{BookRepository, CategoryRepository}

import scala.concurrent.{ExecutionContext, Future}


class BookSpecHelper(categoryRepository: CategoryRepository)(bookRepository: BookRepository)(implicit executor: ExecutionContext) {

  val category = Category(None, "Romance")

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

  def bulkInsert: Future[Seq[Book]] = {
    for {
      s <- categoryRepository.create(sciFiCategory)
      t <- categoryRepository.create(techCategory)
      b <- bookRepository.bulkCreate(bookFields.map { bookField =>
        // Get the respective category id
        val cId = if (bookField._2 == sciFiCategory.title) s.id.get else t.id.get
        book(cId, bookField._1, bookField._3, bookField._4)
      })
    } yield b
  }

  def bulkDelete = {
    for {
      books <- bookRepository.all
      _ <- Future.sequence(books.map(b => bookRepository.delete(b.id.get)))
      Some(sciFi) <- categoryRepository.findByTitle(sciFiCategory.title)
      Some(tech) <- categoryRepository.findByTitle(techCategory.title)
      _ <- categoryRepository.delete(sciFi.id.get)
      _ <- categoryRepository.delete(tech.id.get)
    } yield books
  }

  def bulkInsertAndDelete(assertion: Seq[Book] => Future[Assertion]): Future[Assertion] = {
    bulkInsert.flatMap { books =>
      val a = assertion(books)
      bulkDelete.flatMap(_ => a)
    }
  }

  def book(categoryId: Long, title: String = "Murder in Ganymede", releaseDate: Date = Date.valueOf("1998-01-20"),
           author: String = "John Doe", price: Double = 10.0) =
    Book(None, title, releaseDate, categoryId, 3, price, author)

  // Helper function to create a category,then a book, perform some assertions to it, and then delete them both.
  def createAndDelete[T]()(assertion: Book => Future[T]): Future[T] = {
    // We first start with a category so we have a valid category id for our book
    categoryRepository.create(category) flatMap { c =>
      // With a valid category, we can now create a book
      bookRepository.create(book(c.id.get)) flatMap { b =>
        // Perform the necessary assertions to our created book
        val assertions = assertion(b)
        // Delete the book first, because of the foreign key
        bookRepository.delete(b.id.get).flatMap { _ =>
          // We can finally delete the category, and return the result of the assertions
          categoryRepository.delete(c.id.get) flatMap  { _ => assertions }
        }
      }
    }
  }

}
