package repositories

import scala.concurrent.{ExecutionContext, Future}

import models._
import services.DatabaseService


class BookRepository (val databaseService: DatabaseService)(implicit executor: ExecutionContext) extends BookTable {
  import databaseService._
  import databaseService.driver.api._

  // Here we just return the whole table query
  def all: Future[Seq[Book]] = db.run(books.result)

  // We add the book to our existing table query
  def create(book: Book): Future[Book] = db.run(books returning books += book)

  def bulkCreate(bookSeq: Seq[Book]): Future[Seq[Book]] = {
    println("Bulk Creating")
    val res = db.run(books returning books ++= bookSeq)
    println("Bulk Creating Done")
    res
  }

  // Here we just filter our table query by id
  def findById(id: Long): Future[Option[Book]] = db.run(books.filter(_.id === id).result.headOption)

  def search(bookSearch: BookSearch): Future[Seq[Book]] = {
    val query = books.filter { book =>
      List(
        bookSearch.title.map(t => book.title like s"%$t%"),
        bookSearch.releaseDate.map(book.releaseDate === _),
        bookSearch.categoryId.map(book.categoryId === _),
        bookSearch.author.map(a => book.author like s"%$a%")
      ).collect({case Some(criteria) => criteria}).reduceLeftOption(_ && _).getOrElse(true: Rep[Boolean])
    }

    db.run(query.result)
  }

  // Here we find the respective book and then we delete it
  def delete(id: Long): Future[Int] = db.run(books.filter(_.id === id).delete)

  def close = db.close()
}
