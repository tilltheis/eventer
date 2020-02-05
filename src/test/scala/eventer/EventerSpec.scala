package eventer

import eventer.application.{CodecsSpec, WebServerSpec}
import eventer.domain.{BlowfishCryptoHashingSpec, SessionServiceImplSpec}
import eventer.infrastructure.{
  DatabaseContext,
  DatabaseProvider,
  DbEventRepositorySpec,
  DbUserRepositorySpec,
  TestDatabaseProvider
}
import zio.blocking.Blocking
import zio.test._
import zio.{Cause, ZManaged}

object EventerSpec
    extends DefaultRunnableSpec(
      suite("Eventer")(suite("Unit")(EventerSpecs.unitSpecs: _*),
                       suite("Database")(EventerSpecs.providedDatabaseSpecs: _*) @@ TestAspect.sequential))

object EventerSpecs {
  val unitSpecs: Seq[UnitSpec] =
    Seq(CodecsSpec.spec, WebServerSpec.spec, BlowfishCryptoHashingSpec.spec, SessionServiceImplSpec.spec)
  val databaseSpecs: Seq[DatabaseSpec] = Seq(DbEventRepositorySpec.spec, DbUserRepositorySpec.spec)

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
    }))
}
