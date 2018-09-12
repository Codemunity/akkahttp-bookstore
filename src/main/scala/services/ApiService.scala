package services

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.Directives._
import controllers.{BookController, CategoryController}
import repositories.{BookRepository, CategoryRepository}


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

  def routes =
    pathPrefix("api") {
      categoryController.routes ~
      bookController.routes ~
      authController.routes ~
      userController.routes
    }

}