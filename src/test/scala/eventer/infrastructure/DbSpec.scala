package eventer.infrastructure

import eventer.{DbConfig, EventerSpec}
import zio.ZLayer
import zio.blocking.Blocking
import zio.test.environment.TestEnvironment
import zio.test.{TestAspect, ZSpec}

trait DbSpec extends EventerSpec {
  type DbEnv = DatabaseContext with TestEnvironment
  type DbEnvSpec = ZSpec[DbEnv, Any]

  def dbSpec: ZSpec[DbEnv, Any]

  override final def spec: ZSpec[TestEnvironment, Any] = {
    val dependencies = ZLayer.requires[Blocking] ++ ZLayer.succeed(DbConfig("quill"))
    val dbConnection = dependencies >+> DatabaseProvider.live >>> TestDatabaseContext.withDroppedSchemaAndMigration
    dbSpec.provideCustomLayer(dbConnection) @@ TestAspect.sequential
  }
}
