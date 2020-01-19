package eventer.domain

import zio.RIO

trait EventRepository[R] {
  def findEvents: RIO[R, Seq[Event]]
  def createTestEvents: RIO[R, Unit]
}
