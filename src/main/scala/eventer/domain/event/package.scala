package eventer.domain

import zio.Has

package object event {
  type EventRepository = Has[EventRepository.Service]
}
