package eventer

import eventer.application.{CodecsSpec, WebServerSpec}
import eventer.domain.{BlowfishCryptoHashingSpec, SessionServiceImplSpec}
import eventer.infrastructure.{DbEventRepositorySpec, DbUserRepositorySpec}
import zio.test._
import zio.test.environment.TestEnvironment

object EventerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Eventer")(
      suite("Unit")(CodecsSpec.spec, WebServerSpec.spec, BlowfishCryptoHashingSpec.spec, SessionServiceImplSpec.spec),
      suite("Database")(DbEventRepositorySpec.spec, DbUserRepositorySpec.spec) @@ TestAspect.sequential
    )

}
