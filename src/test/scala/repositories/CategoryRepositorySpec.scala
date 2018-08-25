package repositories

import helpers.CategorySpecHelper
import org.scalatest.{AsyncWordSpec, BeforeAndAfterAll, MustMatchers}
import repository.CategoryRepository
import services.{ConfigService, FlywayService, PostgresService}

// We use AyncWordSpec to be able to test Future[Assertion]: http://www.scalatest.org/user_guide/async_testing
class CategoryRepositorySpec extends AsyncWordSpec
  with MustMatchers
  with BeforeAndAfterAll
  with ConfigService {

  /*
    Instantiate our service in charge of migrating our schema.
    We need to make sure it exists before all our tests are ran,
    and we also need to make sure our schema is destroyed after all tests are ran,
     because we need to always start from a clean slate.
   */
  val flywayService = new FlywayService(jdbcUrl, dbUser, dbPassword)

  // We need a service that provides us access to our database
  val databaseService = new PostgresService(jdbcUrl, dbUser, dbPassword)

  // We need access to our "categories" table
  val categoryRepository = new CategoryRepository(databaseService)

  // Our class for category-related helper methods
  val categorySpecHelper = new CategorySpecHelper(categoryRepository)

  override def beforeAll {
    // Let's make sure our schema is created
    flywayService.migrateDatabase
  }

  override def afterAll {
    // Let's make sure our schema is dropped
    flywayService.dropDatabase
  }


  "A CategoryRepository" must {

    "be empty at the beginning" in {
      categoryRepository.all map { cs => cs.size mustBe 0 }
    }

    "create valid categories" in {

      categorySpecHelper.createAndDelete() { c =>
        c.id mustBe defined
        categoryRepository.all map { cs => cs.size mustBe 1 }
      }
    }

    "not find a category by title if it doesn't exist" in {
      categoryRepository.findByTitle("not a valid title") map { c => c must not be defined }
    }

    "find a category by title if it exists" in {
      categorySpecHelper.createAndDelete() { c =>
        categoryRepository.findByTitle(c.title) map { c => c mustBe defined }
      }
    }

    "delete a category by id if it exists" in {
      categoryRepository.create(categorySpecHelper.category) flatMap { c =>
        categoryRepository.delete(c.id.get) flatMap { _ =>
          categoryRepository.all map { cs => cs.size mustBe 0 }
        }
      }
    }

  }
}
