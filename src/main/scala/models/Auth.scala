package models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


case class Auth(user: User, token: String)

trait AuthJson extends SprayJsonSupport with DefaultJsonProtocol with UserJson {
  implicit val authFormat = jsonFormat2(Auth.apply)
}