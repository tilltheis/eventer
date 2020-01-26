package eventer

import eventer.application.{CodecsSpec, WebServerSpec}
import eventer.infrastructure.{DatabaseContext, DatabaseProvider, DbEventRepositorySpec, TestDatabaseProvider}
import zio.blocking.Blocking
import zio.test._
import zio.{Cause, ZManaged}

object EventerSpec
    extends DefaultRunnableSpec(suite("Eventer")((EventerSpecs.unitSpecs ++ EventerSpecs.providedDatabaseSpecs): _*))

object EventerSpecs {
  val unitSpecs: Seq[UnitSpec] = Seq(CodecsSpec.spec, WebServerSpec.spec)
  val databaseSpecs: Seq[DatabaseSpec] = Seq(DbEventRepositorySpec.spec)

  val managedDatabaseContext: ZManaged[Any, TestFailure.Runtime[Throwable], DatabaseContext] =
    ZManaged
      .environment[DatabaseProvider]
      .flatMap(_.databaseProvider.database)
      .provide(new TestDatabaseProvider.WithDroppedSchemaAndMigration("quill") with Blocking.Live)
      .mapError(e => TestFailure.Runtime(Cause.fail(e)))

  val providedDatabaseSpecs: Seq[TestEnvSpec] =
    databaseSpecs.map(_.provideSomeManaged[Blocking, TestFailure[Throwable]](managedDatabaseContext.flatMap { ctx =>
      ZManaged.environment[Blocking].map { env =>
        new DatabaseContext with Blocking {
          override val blocking: Blocking.Service[Any] = env.blocking
          override def databaseContext: DatabaseContext.Service = ctx.databaseContext
        }
      }
    }) @@ TestAspect.sequential)
}
