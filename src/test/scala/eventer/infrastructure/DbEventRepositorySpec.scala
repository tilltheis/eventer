package eventer.infrastructure

import java.util.UUID

import eventer.DatabaseSpec
import eventer.domain.{EventId, TestData}
import zio.test.Assertion._
import zio.test._

object DbEventRepositorySpec {
  val repository = new DbEventRepository()
  val otherId = EventId(UUID.fromString("b10c8d00-659e-4d2a-aa3b-0aea3d60c2ca"))

  val spec: DatabaseSpec = suite("DbEventRepository")(
    suite("findAll")(
      testM("returns all events from the DB") {
        for {
          _ <- repository.create(TestData.event)
          _ <- repository.create(TestData.event.copy(id = otherId))
          foundEvents <- repository.findAll
        } yield assert(foundEvents, equalTo(Seq(TestData.event, TestData.event.copy(id = otherId))))
      },
      testM("returns nothing if the DB is empty") {
        assertM(repository.findAll, isEmpty)
      }
    ),
    suite("findById")(
      testM("returns the event with the given id if it exists") {
        for {
          _ <- repository.create(TestData.event)
          foundEvent <- repository.findById(TestData.event.id)
        } yield assert(foundEvent, isSome(equalTo(TestData.event)))
      },
      testM("returns nothing if the DB is empty") {
        for {
          foundEvent <- repository.findById(TestData.event.id)
        } yield assert(foundEvent, isNone)
      },
      testM("returns nothing if no event with the given id exists") {
        for {
          _ <- repository.create(TestData.event)
          foundEvent <- repository.findById(otherId)
        } yield assert(foundEvent, isNone)
      },
    ),
    suite("create")(
      testM("inserts the given event into the DB") {
        for {
          _ <- repository.create(TestData.event)
          foundEvent <- repository.findAll
        } yield assert(foundEvent, equalTo(Seq(TestData.event)))
      }
    )
  )
}
