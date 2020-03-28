package eventer.infrastructure

import java.util.UUID

import eventer.domain.{EventId, TestData}
import eventer.{TestEnvSpec, dbTestM}
import zio.test.Assertion._
import zio.test._

object DbEventRepositorySpec {
  private val userRepository = new DbUserRepository[String](identity, identity)
  private val eventRepository = new DbEventRepository()
  private val otherId = EventId(UUID.fromString("b10c8d00-659e-4d2a-aa3b-0aea3d60c2ca"))

  val spec: TestEnvSpec = suite("DbEventRepository")(
    suite("create")(
      dbTestM("inserts the given event into the DB") {
        for {
          _ <- userRepository.create(TestData.user)
          _ <- eventRepository.create(TestData.event)
          foundEvent <- eventRepository.findAll
        } yield assert(foundEvent)(equalTo(Seq(TestData.event)))
      }
    ),
    suite("findAll")(
      dbTestM("returns all events from the DB") {
        for {
          _ <- userRepository.create(TestData.user)
          _ <- eventRepository.create(TestData.event)
          _ <- eventRepository.create(TestData.event.copy(id = otherId))
          foundEvents <- eventRepository.findAll
        } yield assert(foundEvents)(equalTo(Seq(TestData.event, TestData.event.copy(id = otherId))))
      },
      dbTestM("returns nothing if the DB is empty") {
        assertM(eventRepository.findAll)(isEmpty)
      }
    ),
    suite("findById")(
      dbTestM("returns the event with the given id if it exists") {
        for {
          _ <- userRepository.create(TestData.user)
          _ <- eventRepository.create(TestData.event)
          foundEvent <- eventRepository.findById(TestData.event.id)
        } yield assert(foundEvent)(isSome(equalTo(TestData.event)))
      },
      dbTestM("returns nothing if the DB is empty") {
        for {
          foundEvent <- eventRepository.findById(TestData.event.id)
        } yield assert(foundEvent)(isNone)
      },
      dbTestM("returns nothing if no event with the given id exists") {
        for {
          _ <- userRepository.create(TestData.user)
          _ <- eventRepository.create(TestData.event)
          foundEvent <- eventRepository.findById(otherId)
        } yield assert(foundEvent)(isNone)
      },
    )
  )
}
