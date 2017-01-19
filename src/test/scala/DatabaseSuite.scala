import org.scalatest.Sequential
import repositories.{BookRepositorySpec, BookSearchSpec, CategoryRepositorySpec}

class DatabaseSuite extends Sequential(
  new CategoryRepositorySpec,
  new BookRepositorySpec,
  new BookSearchSpec,
  new BookEndpointSpec,
  new CategoryEndpointSpec
) {

}
