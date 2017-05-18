package controllers

import java.sql.Date

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import models.{Book, BookSearch, BookSearchJson}
import repository.{BookRepository, CategoryRepository}
import views.BookSearchView

import scala.concurrent.{ExecutionContext, Future}


class BookViewSearchController(val categoryRepository: CategoryRepository, val bookRepository: BookRepository)(implicit val ec: ExecutionContext) extends BookSearchJson {

  val routes = pathEndOrSingleSlash {
    get {
      complete {
        respondWithView(bookRepository.all)
      }
    } ~
      post {
        decodeRequest {
          formFields('title.?, 'author.?, 'releaseDate.?, 'categoryId.as[Long].?, 'currency) {
            (title, author, releaseDate, categoryId, currency) =>
              complete {
                val t = emptyStringToNone(title)
                val a = emptyStringToNone(author)
                val r = emptyStringToNone(releaseDate)
                val bookSearch = BookSearch(t, r.map(Date.valueOf), categoryId, a)

                respondWithView(bookRepository.search(bookSearch))
              }
          }
        }
      }
  }

  def respondWithView(booksFuture: Future[Seq[Book]]): Future[HttpResponse] = {
    for {
      categories <- categoryRepository.all
      books <- booksFuture
    } yield {
      val currencies = Seq("USD", "EUR")
      HttpResponse(entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        BookSearchView.view(categories, currencies, books)))
    }
  }

  def emptyStringToNone(str: Option[String]): Option[String] =
    str.fold[Option[String]](None)(s => if (s.isEmpty) None else Some(s))

}