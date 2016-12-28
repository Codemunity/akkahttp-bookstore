package helpers

import java.sql.Date

import models.{Book, Category}
import repository.{BookRepository, CategoryRepository}

import scala.concurrent.{ExecutionContext, Future}


class BookSpecHelper(categoryRepository: CategoryRepository)(bookRepository: BookRepository)(implicit executor: ExecutionContext) {

  val category = Category(None, "Sci-Fi")

  def book(categoryId: Long, title: String = "Murder in Ganymede", releaseDate: Date = Date.valueOf("1998-01-20"), author: String = "John Doe") =
    Book(None, title, releaseDate, categoryId, 3, author)

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
