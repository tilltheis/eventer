package eventer

import eventer.application.{CodecsSpec, WebServerSpec}
import eventer.infrastructure.{DatabaseContext, DatabaseProvider, DbEventRepositorySpec, TestDatabaseProvider}
import zio.blocking.Blocking
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Cause, ZManaged}

object EventerSpec
    extends DefaultRunnableSpec(suite("Eventer")((EventerSpecs.unitTests ++ EventerSpecs.databaseTests): _*))

object EventerSpecs {
  val managedDatabaseContext: ZManaged[Any, TestFailure.Runtime[Throwable], DatabaseContext] =
    ZManaged
      .environment[DatabaseProvider]
      .flatMap(_.databaseProvider.database)
      .provide(new Blocking.Live with TestDatabaseProvider.WithDroppedSchemaAndMigration)
      .mapError(e => TestFailure.Runtime(Cause.fail(e)))

  type Specs = Seq[ZSpec[TestEnvironment, Throwable, String, Unit]]
  val unitTests: Specs = Seq(CodecsSpec.spec, WebServerSpec.spec)
  val databaseTests: Specs =
    Seq(DbEventRepositorySpec.spec).map(_.provideManaged(managedDatabaseContext) @@ TestAspect.sequential)
}
