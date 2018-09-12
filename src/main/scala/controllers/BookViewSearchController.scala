package controllers

import java.sql.Date

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import models.{Book, BookSearch, BookSearchJson}
import repositories.{BookRepository, CategoryRepository}
import services.CurrencyService
import views.BookSearchView


class BookViewSearchController(val categoryRepository: CategoryRepository, val bookRepository: BookRepository)(implicit val ec: ExecutionContext, as: ActorSystem, mat: Materializer) extends BookSearchJson {

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

                val parsedDate = Try(r.map(Date.valueOf))
                parsedDate match {
                  case Failure(e) =>
                    println(e)
                    val errors = List("Invalid date format, please enter a valid one. YYYY-MM-DD")
                    respondWithView(bookRepository.all, CurrencyService.baseCurrency, errors)
                  case Success(date) =>
                    val bookSearch = BookSearch(t, date, categoryId, a)

                    // Our default future
                    val bookSearchQuery = bookRepository.search(bookSearch)

                    // Our updated future, if the currency is not the base one, we will modify it
                    val booksFuture = if (currency != CurrencyService.baseCurrency) {
                      bookSearchQuery.flatMap { books =>
                        CurrencyService.getRates.map { ratesOpt =>
                          // If our rates map is empty, we will return the default `books` list returned by the default
                          // future, meaning no change was done
                          ratesOpt.fold(books)(rates => {
                            // Again, if the map is not empty, but there is no exchange rate for the currency,
                            // we return the default `books` list, otherwise we change the original list
                            // with the given exchange rate
                            rates.get(currency).fold(books)(rate => books.map(b => b.copy(price = b.price * rate)))
                          })
                        }
                      }
                    }
                    // Otherwise we will use the default one
                    else bookSearchQuery

                    respondWithView(booksFuture, currency)
                }
              }
          }
        }
      }
  }

  def respondWithView(booksFuture: Future[Seq[Book]], currentCurrency: String = CurrencyService.baseCurrency, errors: List[String] = List()): Future[HttpResponse] = {
    for {
      categories <- categoryRepository.all
      books <- booksFuture
    } yield {
      val currencies = CurrencyService.supportedCurrencies
      HttpResponse(entity = HttpEntity(
        ContentTypes.`text/html(UTF-8)`,
        BookSearchView.view(categories, currencies, books, errors, currentCurrency)))
    }
  }

  def emptyStringToNone(str: Option[String]): Option[String] =
    str.fold[Option[String]](None)(s => if (s.isEmpty) None else Some(s))

}
