package repositories

import models._
import services.DatabaseService

import scala.concurrent.{ExecutionContext, Future}


class CategoryRepository (val databaseService: DatabaseService)(implicit executor: ExecutionContext) extends CategoryTable {
  import databaseService._
  import databaseService.driver.api._

  // Here we just return the whole table query
  def all: Future[Seq[Category]] = db.run(categories.result)

  // We add the category to our existing table query
  def create(category: Category): Future[Category] = db.run(categories returning categories += category)

  // Here we just filter our table query by title
  def findByTitle(title: String): Future[Option[Category]] = db.run(categories.filter(_.title === title).result.headOption)

  // Here we find the respective category and then we delete it
  def delete(id: Long): Future[Int] = db.run(categories.filter(_.id === id).delete)
}
