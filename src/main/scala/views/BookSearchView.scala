package views

import models.{Book, Category}
import services.CurrencyService

import scalatags.Text
import scalatags.Text.all._

object BookSearchView {

  def view(categories: Seq[Category], currencies: Seq[String], books: Seq[Book], errors: List[String] = List(), currentCurrency: String = CurrencyService.baseCurrency) = html(
    head(

    ),
    body(
      h1("Search our Inmense Catalog!"),
      ul(errors.map(e => li(p(e)))),
      form(action:="/books", method:="POST")(
        p(input(name:="title", placeholder:="Title")),
        p(input(name:="releaseDate", placeholder:="Release Date")),
        p(input(name:="author", placeholder:="Author")),
        p(select(name:="categoryId")(
          option,
          categories.map(c => option(value:=c.id.get)(c.title)))
        ),
        p(select(name:="currency")(currencies.map(c => option(if (c == currentCurrency) selected else List.empty[Text.Modifier])(c)))),
        p(input(`type`:="submit", value:="Submit"))
      ),
      if (books.isEmpty) p("No books were found that matched your criteria.")
      else
        ul(
          books.map(b => li(p(s"${b.title} - ${formatPrice(currentCurrency, b.price)}")))
        )
    )
  ).toString()

  def formatPrice(currency: String, price: Double): String = {
    val symbol = CurrencyService.currencySymbols.getOrElse(currency, "$")
    s"$symbol%.2f".format(price)
  }

}