package services

import repository.{AuthRepository, BookRepository, CategoryRepository, UserRepository}
import akka.http.scaladsl.server.Directives._
import controllers._

import scala.concurrent.ExecutionContext


class ApiService(
                  categoryRepository: CategoryRepository,
                  bookRepository: BookRepository,
                  authRepository: AuthRepository,
                  userRepository: UserRepository,
                  tokenService: TokenService
                )(implicit executor: ExecutionContext) {

  val categoryController = new CategoryController(categoryRepository)
  val bookController = new BookController(bookRepository, tokenService)
  val authController = new AuthController(authRepository, tokenService)
  val userController = new UserController(userRepository, tokenService)

  val bookViewSearchController = new BookViewSearchController(categoryRepository, bookRepository)

  def routes =
    // Add a new route
    pathPrefix("books") {
      bookViewSearchController.routes
    } ~
    pathPrefix("api") {
      categoryController.routes ~
      bookController.routes ~
      authController.routes ~
      userController.routes
    }

}