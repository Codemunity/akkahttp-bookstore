package controllers

import akka.http.scaladsl.model.StatusCodes

import akka.http.scaladsl.server.Directives._
import models.{CategoryJson, Category}
import repository.CategoryRepository

class CategoryController(val categoryRepository: CategoryRepository) extends CategoryJson {

  val routes = pathPrefix("categories") {
    pathEndOrSingleSlash {
      get {
        complete {
          categoryRepository.all
        }
      } ~
        post {
          // From it's documentation: Decompresses the incoming request if it is `gzip` or `deflate` compressed. Uncompressed requests are passed through untouched.
          decodeRequest {
            // Parses the request as the given entity, in this case a `Category`
            entity(as[Category]) { category =>
              // We first check whether a category with same title exists
              onSuccess(categoryRepository.findByTitle(category.title)) {
                // In case it does exist, we return a `BadRequest` status
                case Some(_) => complete(StatusCodes.BadRequest)
                // If it doesn't exist, we create the category
                case None => complete(StatusCodes.Created, categoryRepository.create(category))
              }
            }
          }
        }
    }  ~
      // After the `/categories/` we expect the id of the category we want to delete
      pathPrefix(IntNumber) { id =>
        // We want to listen to paths like `/categories/id` or `/categories/id/`
        pathEndOrSingleSlash {
          delete {
            onSuccess(categoryRepository.delete(id)) {
              // If we get a number higher than 0, it deleted the category
              case n if n > 0 => complete(StatusCodes.NoContent)
              // If 0 is returned, the category wasn't found
              case _ => complete(StatusCodes.NotFound)
            }
          }
        }
      }
  }

}