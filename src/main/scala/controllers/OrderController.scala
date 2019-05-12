package controllers

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.unmarshalling.PredefinedFromStringUnmarshallers
import directives.VerifyToken
import models._
import repositories.OrderRepository
import services.TokenService
import akka.http.scaladsl.server.Directives._

class OrderController(
    val orderRepository: OrderRepository,
    val tokenService: TokenService)(implicit val ec: ExecutionContext)
    extends OrderJson
    with PredefinedFromStringUnmarshallers
    with VerifyToken {

  val routes = pathPrefix("orders") {
    verifyToken { user =>
      pathEndOrSingleSlash {
        post {
          (decodeRequest & entity(as[OrderWithBooks])) { createOrder: OrderWithBooks =>
            complete(StatusCodes.Created, orderRepository.createOrder(createOrder))
          }
        } ~ get {
          complete(orderRepository.findOrdersByUser(user.id.get))
        }
      } ~ path(IntNumber / "books") { orderId =>
        get {
          complete(orderRepository.findBooksByOrder(orderId.toLong))
        }
      }
    }
  }
}
