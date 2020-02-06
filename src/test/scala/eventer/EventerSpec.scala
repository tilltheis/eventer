package eventer

import eventer.application.{CodecsSpec, WebServerSpec}
import eventer.domain.{BlowfishCryptoHashingSpec, SessionServiceImplSpec}
import eventer.infrastructure.{DbEventRepositorySpec, DbUserRepositorySpec}
import zio.test._

object EventerSpec
    extends DefaultRunnableSpec(
      suite("Eventer")(
        suite("Unit")(CodecsSpec.spec, WebServerSpec.spec, BlowfishCryptoHashingSpec.spec, SessionServiceImplSpec.spec),
        suite("Database")(DbEventRepositorySpec.spec, DbUserRepositorySpec.spec) @@ TestAspect.sequential
      ))
