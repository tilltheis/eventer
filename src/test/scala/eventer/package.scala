import eventer.infrastructure.{DatabaseContext, DatabaseProvider, TestDatabaseProvider}
import zio.{Cause, ZIO, ZManaged}
import zio.blocking.Blocking
import zio.test.{TestFailure, TestResult, ZSpec, testM}
import zio.test.environment.TestEnvironment

package object eventer {
  type TestEnvSpec = ZSpec[TestEnvironment, Throwable, String, Unit]

  final def dbTestM[L](label: L)(
      assertion: => ZIO[DatabaseContext with Blocking, Throwable, TestResult]): ZSpec[Blocking, Throwable, L, Unit] =
    someDbTestM { ctx =>
      ZManaged.environment[Blocking].map { env =>
        new DatabaseContext with Blocking {
          override val blocking: Blocking.Service[Any] = env.blocking
          override def databaseContext: DatabaseContext.Service = ctx.databaseContext
        }
      }
    }(label)(assertion)

  final def someDbTestM[R0 <: Blocking, R <: DatabaseContext with Blocking, L](
      makeEnv: DatabaseContext => ZManaged[R0, TestFailure[Throwable], R])(label: L)(
      assertion: => ZIO[R, Throwable, TestResult]): ZSpec[R0, Throwable, L, Unit] =
    testM(label)(assertion).provideSomeManaged(managedDatabaseContext.flatMap(makeEnv))

  private val managedDatabaseContext: ZManaged[Blocking, TestFailure[Throwable], DatabaseContext] =
    ZManaged
      .environment[DatabaseProvider]
      .flatMap(_.databaseProvider.database)
      .mapError(e => TestFailure.Runtime(Cause.fail(e)))
      .provideSome[Blocking] { envBlocking =>
        new TestDatabaseProvider.WithDroppedSchemaAndMigration("quill") with Blocking {
          override val blocking: Blocking.Service[Any] = envBlocking.blocking
        }
      }
}
