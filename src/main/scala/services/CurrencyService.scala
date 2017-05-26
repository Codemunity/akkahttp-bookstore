package services

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpMethods, HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import spray.json.DefaultJsonProtocol

import scala.concurrent.{ExecutionContext, Future}

// Our Currency service, won't leak implementation details
object CurrencyService {

  // The response from the current API, private since it is related directly to the API,
  // meaning that if the API changes, so will the response (probably)
  private case class FixerResponse(rates: Map[String, Float])

  // Our custom JSON protocol, this time we use an object because we will use it only in
  // our function, so it will be imported and not extended
  private object FixerResponseJson extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val fixerResponseJsonFormat = jsonFormat1(FixerResponse.apply)
  }

  // Our base currency
  val baseCurrency = "USD"
  /// All of our supported currencies, this time we won't have it hardcoded in the controller
  val supportedCurrencies = Seq("USD", "EUR")
  // The currencies that require exchange rates, we don't require our base currency
  private val exchangeCurrencies = supportedCurrencies.filterNot(_ == baseCurrency)
  // The url built using the previous values
  private val requestUrl = s"http://api.fixer.io/latest?base=$baseCurrency&symbols=${exchangeCurrencies.mkString(",")}"
  // The request won't be executed right away, think of it as a blueprint
  private val request = HttpRequest(HttpMethods.GET, requestUrl)

  // Http requires implicit parameters, so we must provide them
  def getRates(implicit ec: ExecutionContext, as: ActorSystem, mat: Materializer): Future[Option[Map[String, Float]]] =
  // The request is executed until now
    Http().singleRequest(request).flatMap { response =>
      response.status match {
        // If the response code is OK, and the response body is JSON, we can parse it
        case StatusCodes.OK if response.entity.contentType == ContentTypes.`application/json` =>
          import spray.json._
          import FixerResponseJson._

          // First, we need to transform the response entity or body to a String
          Unmarshal(response.entity).to[String].map { jsonString =>
            // Then we parse the JSON String to our FixerResponse model, and extract it's rates
            val rates = jsonString.parseJson.convertTo[FixerResponse].rates
            // If no rate was returned, we also notify that something unexpected happened
            if (rates.isEmpty) None
            // If everything went well, we just return the rates
            else Some(rates)
          }
        // Otherwise notify the caller that something unexpected happened
        case _ =>
          Future.successful(None)
      }
    }

}
