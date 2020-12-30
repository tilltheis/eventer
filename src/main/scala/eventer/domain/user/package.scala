package eventer.domain

import zio.Has

package object user {
  type UserRepository[HashT] = Has[UserRepository.Service[HashT]]
}
