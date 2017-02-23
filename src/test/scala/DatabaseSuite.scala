import org.scalatest.Sequential
import repositories._

class DatabaseSuite extends Sequential(
  new CategoryRepositorySpec,
  new BookRepositorySpec,
  new UserRepositorySpec,
  new AuthRepositorySpec,
  new BookSearchSpec,
  new BookEndpointSpec,
  new CategoryEndpointSpec,
  new UserEndpointSpec
) {

}
