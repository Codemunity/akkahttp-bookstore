package models

import java.sql.Timestamp

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.driver.PostgresDriver.api._
import spray.json.DefaultJsonProtocol

case class OrderWithBooks(
    order: Order,
    books: Seq[Book]
)

case class Order(
    id: Option[Long] = None,
    orderDate: Timestamp,
    userId: Long,
    totalPrice: Double
)

case class BookByOrder(
    id: Option[Long] = None,
    orderId: Long,
    bookId: Long,
    unitPrice: Double,
    quantity: Int
)

trait OrderJson
    extends SprayJsonSupport
    with DefaultJsonProtocol
    with BookJson {
  import services.FormatService._

  implicit val orderFormat = jsonFormat4(Order.apply)
  implicit val createOrderFormat = jsonFormat2(OrderWithBooks.apply)
  implicit val bookByOrderFormat = jsonFormat5(BookByOrder.apply)
}

trait OrderTable {

  class Orders(tag: Tag) extends Table[Order](tag, "orders") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def orderDate = column[Timestamp]("order_date")
    def userId = column[Long]("user_id")
    def totalPrice = column[Double]("total_price_usd")

    def * =
      (id, orderDate, userId, totalPrice) <> ((Order.apply _).tupled, Order.unapply)
  }

  protected val orders = TableQuery[Orders]
}

trait BookByOrderTable {

  class BooksByOrder(tag: Tag)
      extends Table[BookByOrder](tag, "books_by_order") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def orderId = column[Long]("order_id")
    def bookId = column[Long]("book_id")
    def unitPrice = column[Double]("unit_price_usd")
    def quantity = column[Int]("quantity")

    def * =
      (id, orderId, bookId, unitPrice, quantity) <> ((BookByOrder.apply _).tupled, BookByOrder.unapply)
  }

  protected val booksByOrder = TableQuery[BooksByOrder]
}
