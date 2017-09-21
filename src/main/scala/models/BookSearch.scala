package models

import java.sql.Date

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


case class BookSearch(
                       title: Option[String] = None,
                       releaseDate: Option[Date] = None,
                       categoryId: Option[Long] = None,
                       author: Option[String] = None
                     )

// JSON format for our Book model, to convert to and from JSON
trait BookSearchJson extends SprayJsonSupport with DefaultJsonProtocol {
  import services.FormatService._

  implicit val bookSearchFormat = jsonFormat4(BookSearch.apply)

}