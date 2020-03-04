import java.util.Base64

import eventer.infrastructure.{DatabaseContext, DatabaseProvider, TestDatabaseProvider}
import zio.blocking.Blocking
import zio.test.environment.TestEnvironment
import zio.test.{TestResult, ZSpec, testM}
import zio.{Task, ZIO, ZLayer}

package object eventer {
  type TestEnvSpec = ZSpec[TestEnvironment, Any]

  final def dbTestM[E](label: String)(
      assertion: => ZIO[DatabaseContext with Blocking, E, TestResult]): ZSpec[Blocking, E] = {
    val dbLayer = TestDatabaseProvider.withDroppedSchemaAndMigration("quill")
    val ctxLayer = ZLayer.fromService[DatabaseProvider.Service, DatabaseContext.Service](_.database.get)
    val combinedDbLayer = dbLayer >>> ctxLayer
    val depsInput = ZLayer.requires[Blocking] ++ combinedDbLayer
    val providedAssertion = assertion.provideLayer(depsInput)
    testM(label)(providedAssertion)
  }

  def base64Encode(string: String): String = new String(Base64.getEncoder.encode(string.getBytes("UTF-8")), "UTF-8")
  def base64Decode(string: String): Task[String] =
    Task(new String(Base64.getDecoder.decode(string.getBytes("UTF-8")), "UTF-8"))
}
