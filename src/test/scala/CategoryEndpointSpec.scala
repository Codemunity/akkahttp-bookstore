import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import controllers.CategoryController
import helpers.CategorySpecHelper
import models.{Category, CategoryJson}
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repositories.CategoryRepository
import services.{ConfigService, FlywayService, PostgresService}

class CategoryEndpointSpec extends AsyncWordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with ConfigService
  with WebApi
  with ScalatestRouteTest
  with CategoryJson
{

  override implicit val executor = system.dispatcher

  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

  val databaseService = new PostgresService(jdbcUrl, dbUser, dbPassword)

  val categoryRepository = new CategoryRepository(databaseService)

  val categorySpecHelper = new CategorySpecHelper(categoryRepository)

  val categoryController = new CategoryController(categoryRepository)

  override def beforeAll {
    // Let's make sure our schema is created
    flywayService.migrateDatabase
  }

  override def afterAll {
    // Let's make sure our schema is dropped
    flywayService.dropDatabase
  }

  "A CategoryEndpoint" must {

    "return an empty list at the beginning" in {
      Get("/categories/") ~> categoryController.routes ~> check {
        status mustBe StatusCodes.OK
        // Parse the response as a list of categories
        val categories = responseAs[List[Category]]
        // Let's assert that the list must have a size of 0
        categories must have size 0
      }
    }

    "return all the categories when there is at least one" in {
      categorySpecHelper.createAndDelete() { c =>
        Get("/categories/") ~> categoryController.routes ~> check {
          status mustBe StatusCodes.OK
          // Parse the response as a list of categories
          val categories = responseAs[List[Category]]
          // Let's assert that the list must have a size of 1
          categories must have size 1
        }
      }
    }

    "return BadRequest with repeated titles" in {
      categorySpecHelper.createAndDelete() { c =>
        Post("/categories/", categorySpecHelper.category) ~> categoryController.routes ~> check {
          // Assert that with repeated titles we must get a bad request
          status mustBe StatusCodes.BadRequest
        }
      }
    }

    "create a category" in {
      Post("/categories/", categorySpecHelper.category) ~> categoryController.routes ~> check {
        status mustBe StatusCodes.Created

        // Parse the response as category
        val category = responseAs[Category]

        // Let's delete the category to make sure we don't corrupt the rest of the tests
        categoryRepository.delete(category.id.get)

        // Let's assert that the category has an id, this means it was persisted
        category.id mustBe defined
        // Let's make sure it is the actual category we tried to create
        category.title mustBe categorySpecHelper.category.title
      }
    }

    "return NotFound when we try to delete a non existent category" in {
      Delete("/categories/10/") ~> categoryController.routes ~> check {
        status mustBe StatusCodes.NotFound
      }
    }

    "return NoContent when we delete an existent category" in {
      categoryRepository.create(categorySpecHelper.category) flatMap { c =>
        Delete(s"/categories/${c.id.get}/") ~> categoryController.routes ~> check {
          status mustBe StatusCodes.NoContent
        }
      }
    }

  }
}
