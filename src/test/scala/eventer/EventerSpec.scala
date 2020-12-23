package eventer

import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, ZSpec}

abstract class EventerSpec extends DefaultRunnableSpec {
  type TestEnvSpec = ZSpec[TestEnvironment, Any]
}
