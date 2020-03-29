package eventer

import eventer.application.{CodecsSpec, WebServerSpec}
import eventer.domain.{BlowfishCryptoHashingSpec, SessionServiceImplSpec}
import eventer.infrastructure.{DbEventRepositorySpec, DbUserRepositorySpec, EmailSenderImplSpec}
import zio.test._
import zio.test.environment.TestEnvironment

object EventerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("eventer")(
      suite("application")(WebServerSpec.spec),
      suite("domain")(CodecsSpec.spec, WebServerSpec.spec, BlowfishCryptoHashingSpec.spec, SessionServiceImplSpec.spec),
      suite("infrastructure")(
        suite("database")(DbEventRepositorySpec.spec, DbUserRepositorySpec.spec) @@ TestAspect.sequential,
        EmailSenderImplSpec.spec)
    )

}
