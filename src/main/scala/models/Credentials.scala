package models

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol


case class Credentials(email: String, password: String)

trait CredentialsJson extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val credentialsFormat = jsonFormat2(Credentials.apply)
}