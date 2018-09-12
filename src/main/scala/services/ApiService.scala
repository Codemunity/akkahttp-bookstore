package services

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.Directives._
import controllers.{BookController, CategoryController}
import repositories.{BookRepository, CategoryRepository}


class ApiService(categoryRepository: CategoryRepository, bookRepository: BookRepository)(implicit executor: ExecutionContext) {

  val categoryController = new CategoryController(categoryRepository)

  val bookController = new BookController(bookRepository)

  def routes =
    pathPrefix("api") {
      categoryController.routes ~
      bookController.routes
    }

}
