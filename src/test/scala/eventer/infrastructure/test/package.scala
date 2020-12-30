package eventer.infrastructure

import zio.Has

package object test {
  type InMemoryEventRepository = Has[InMemoryEventRepository.Service]
  type InMemoryUserRepository = Has[InMemoryUserRepository.Service]
}
