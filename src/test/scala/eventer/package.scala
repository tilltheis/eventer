import eventer.infrastructure.DatabaseContext
import zio.blocking.Blocking
import zio.test.ZSpec
import zio.test.environment.TestEnvironment

package object eventer {
  type UnitSpec = ZSpec[Any, Throwable, String, Unit]
  type DatabaseSpec = ZSpec[DatabaseContext with Blocking, Throwable, String, Unit]
  type TestEnvSpec = ZSpec[TestEnvironment, Throwable, String, Unit]
}
