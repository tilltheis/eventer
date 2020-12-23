package eventer.infrastructure

import eventer.EventerSpec
import zio.test.environment.TestEnvironment
import zio.test.{TestAspect, ZSpec}

trait DbSpec extends EventerSpec {
  type DbEnv = DatabaseContext with TestEnvironment
  type DbEnvSpec = ZSpec[DbEnv, Any]

  def dbSpec: ZSpec[DbEnv, Any]

  override final def spec: ZSpec[TestEnvironment, Any] = {
    val layer = TestDatabaseProvider.withDroppedSchemaAndMigration("quill").map(_.get.database)
    dbSpec.provideCustomLayer(layer) @@ TestAspect.sequential
  }
}
