package eventer.infrastructure

import eventer.Config
import pureconfig.ConfigSource
import zio.Task

trait ConfigProvider {
  def configProvider: ConfigProvider.Service
}

object ConfigProvider {
  trait Service {
    def config: Task[Config]
  }

  trait Live extends ConfigProvider {
    override def configProvider: Service = new Service {
      override val config: Task[Config] = {
        import pureconfig.generic.auto._
        Task.effect(ConfigSource.default.at("eventer").loadOrThrow[Config])
      }
    }
  }

  object Live extends Live
}
