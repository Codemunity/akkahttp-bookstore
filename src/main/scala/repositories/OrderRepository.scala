package repositories

import scala.concurrent.{ExecutionContext, Future}

import models._
import services.DatabaseService

class OrderRepository(val databaseService: DatabaseService)(implicit executor: ExecutionContext) extends OrderTable with BookByOrderTable {
  import databaseService._
  import databaseService.driver.api._

  def findOrdersByUser(userId: Long): Future[Seq[Order]] =
    db.run(orders.filter(_.userId === userId).result)

  def findBooksByOrder(orderId: Long): Future[Seq[BookByOrder]] =
    db.run(booksByOrder.filter(_.orderId === orderId).result)

  def createOrder(orderWithBooks: OrderWithBooks): Future[Order] = {
    val dbAction = (for {
      o <- orders returning orders += orderWithBooks.order
      _ <- booksByOrder returning booksByOrder ++= orderWithBooks.books.groupBy(_.id).map { case (Some(_), books) =>
        prepareBook(o.id.get, books.head, books.size)
      }
    } yield o).transactionally
    db.run(dbAction)
  }

  private def prepareBook(orderId: Long, book: Book, quantity: Int): BookByOrder = {
    BookByOrder(
      id = None,
      orderId = orderId,
      bookId = book.id.get,
      unitPrice = book.price,
      quantity = quantity
    )
  }
}
