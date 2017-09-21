package models

import java.sql.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import slick.driver.PostgresDriver.api._
import spray.json.DefaultJsonProtocol

// Our Book model
case class Book(
                 id: Option[Long] = None,
                 title: String,
                 releaseDate: Date,
                 categoryId: Long,
                 quantity: Int,
                // CHANGE1: Add the price attribute
                 price: Double,
                 author: String
               )

// JSON format for our Book model, to convert to and from JSON
trait BookJson extends SprayJsonSupport with DefaultJsonProtocol {
  import services.FormatService._

  // CHANGE2: use `jsonFormat7` instead of `jsonFormat6`
  implicit val bookFormat = jsonFormat7(Book.apply)

}

// Slick table mapped to our Book model
trait BookTable {

  class Books(tag: Tag) extends Table[Book](tag, "books") {
    def id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def releaseDate = column[Date]("release_date")
    def categoryId = column[Long]("category_id")
    def quantity = column[Int]("quantity")
    // CHANGE3: Create the `price` column
    def price = column[Double]("price_usd")
    def author = column[String]("author")

    // CHANGE4: Pass the `price` to the apply function
    def * = (id, title, releaseDate, categoryId, quantity, price, author) <> ((Book.apply _).tupled, Book.unapply)
  }


  protected val books = TableQuery[Books]

}