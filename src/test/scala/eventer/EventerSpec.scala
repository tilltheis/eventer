package eventer

import eventer.application.CodecsSpec
import eventer.infrastructure.{
  BlowfishCryptoHashingSpec,
  DbEventRepositorySpec,
  DbUserRepositorySpec,
  EmailSenderImplSpec
}
import zio.test._
import zio.test.environment.TestEnvironment

object EventerSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("eventer")(
      suite("domain")(CodecsSpec.spec)
    )

}
