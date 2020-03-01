import java.util.Base64

import eventer.infrastructure.{DatabaseContext, DatabaseProvider, TestDatabaseProvider}
import zio.blocking.Blocking
import zio.test.environment.TestEnvironment
import zio.test.{TestFailure, TestResult, ZSpec, testM}
import zio.{Task, ZIO, ZManaged}

package object eventer {
  type TestEnvSpec = ZSpec[TestEnvironment, Any, String, Unit]

  final def dbTestM[E, L](label: L)(
      assertion: => ZIO[DatabaseContext with Blocking, E, TestResult]): ZSpec[Blocking, E, L, Unit] =
    someDbTestM { ctx =>
      ZManaged.environment[Blocking].map { env =>
        new DatabaseContext with Blocking {
          override val blocking: Blocking.Service[Any] = env.blocking
          override def databaseContext: DatabaseContext.Service = ctx.databaseContext
        }
      }
    }(label)(assertion)

  final def someDbTestM[R0 <: Blocking, R <: DatabaseContext with Blocking, E, L](
      makeEnv: DatabaseContext => ZManaged[R0, TestFailure[E], R])(label: L)(
      assertion: => ZIO[R, E, TestResult]): ZSpec[R0, E, L, Unit] =
    testM(label)(assertion).provideSomeManaged(managedDatabaseContext.flatMap(makeEnv))

  private def managedDatabaseContext[E]: ZManaged[Blocking, TestFailure[E], DatabaseContext] =
    ZManaged
      .environment[DatabaseProvider]
      .flatMap(_.databaseProvider.database)
      .provideSome[Blocking] { envBlocking =>
        new TestDatabaseProvider.WithDroppedSchemaAndMigration("quill") with Blocking {
          override val blocking: Blocking.Service[Any] = envBlocking.blocking
        }
      }

  def base64Encode(string: String): String = new String(Base64.getEncoder.encode(string.getBytes("UTF-8")), "UTF-8")
  def base64Decode(string: String): Task[String] =
    Task(new String(Base64.getDecoder.decode(string.getBytes("UTF-8")), "UTF-8"))
}
