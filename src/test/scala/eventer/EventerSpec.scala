package eventer

import eventer.application.CodecsSpec
import eventer.domain.{BlowfishCryptoHashingSpec, SessionServiceImplSpec}
import eventer.infrastructure.{DbEventRepositorySpec, DbUserRepositorySpec, EmailSenderImplSpec}
import zio.test._
import zio.test.environment.TestEnvironment

object EventerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("eventer")(
      suite("domain")(CodecsSpec.spec, BlowfishCryptoHashingSpec.spec, SessionServiceImplSpec.spec),
      suite("infrastructure")(suite("database")(DbUserRepositorySpec.spec) @@ TestAspect.sequential,
                              EmailSenderImplSpec.spec)
    )

}
