package eventer.infrastructure

import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, TestAspect, ZSpec}

trait DbSpec extends DefaultRunnableSpec {
  type DbEnv = DatabaseContext with TestEnvironment
  type DbEnvSpec = ZSpec[DbEnv, Any]

  def dbSpec: ZSpec[DbEnv, Any]

  override final def spec: ZSpec[TestEnvironment, Any] = {
    val layer = TestDatabaseProvider.withDroppedSchemaAndMigration("quill").map(_.get.database)
    dbSpec.provideCustomLayer(layer) @@ TestAspect.sequential
  }
}
