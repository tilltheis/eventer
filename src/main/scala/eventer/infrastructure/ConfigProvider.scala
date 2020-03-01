package eventer.infrastructure

import eventer.Config
import pureconfig.ConfigSource
import zio.UIO

trait ConfigProvider {
  def configProvider: ConfigProvider.Service
}

object ConfigProvider {
  trait Service {
    def config: UIO[Config]
  }

  trait Live extends ConfigProvider {
    override def configProvider: Service = new Service {
      override val config: UIO[Config] = {
        import pureconfig.generic.auto._
        UIO(ConfigSource.default.at("eventer").loadOrThrow[Config])
      }
    }
  }

  object Live extends Live
}
