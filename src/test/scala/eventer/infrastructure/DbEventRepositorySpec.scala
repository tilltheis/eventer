package eventer.infrastructure

import java.util.UUID
import eventer.domain.{EventId, TestData}
import zio.{Has, URIO, ZIO}
import zio.test.Assertion._
import zio.test._

object DbEventRepositorySpec extends DbSpec {
  private val userRepository = new DbUserRepository[String](identity, identity)
  private val otherId = EventId(UUID.fromString("b10c8d00-659e-4d2a-aa3b-0aea3d60c2ca"))

  private val liveEventRepository: URIO[Has[DatabaseContext.Service], DbEventRepository] =
    ZIO.service[DatabaseContext.Service].map(new DbEventRepository(_))

  val dbSpec: DbEnvSpec = suite("DbEventRepository")(
    suite("create")(
      testM("inserts the given event into the DB") {
        for {
          eventRepository <- liveEventRepository
          _ <- userRepository.create(TestData.user)
          _ <- eventRepository.create(TestData.event)
          foundEvent <- eventRepository.findAll
        } yield assert(foundEvent)(equalTo(Seq(TestData.event)))
      }
    ),
    suite("findAll")(
      testM("returns all events from the DB") {
        for {
          eventRepository <- liveEventRepository
          _ <- userRepository.create(TestData.user)
          _ <- eventRepository.create(TestData.event)
          _ <- eventRepository.create(TestData.event.copy(id = otherId))
          foundEvents <- eventRepository.findAll
        } yield assert(foundEvents)(equalTo(Seq(TestData.event, TestData.event.copy(id = otherId))))
      },
      testM("returns nothing if the DB is empty") {
        assertM(liveEventRepository.flatMap(_.findAll))(isEmpty)
      }
    ),
    suite("findById")(
      testM("returns the event with the given id if it exists") {
        for {
          eventRepository <- liveEventRepository
          _ <- userRepository.create(TestData.user)
          _ <- eventRepository.create(TestData.event)
          foundEvent <- eventRepository.findById(TestData.event.id)
        } yield assert(foundEvent)(isSome(equalTo(TestData.event)))
      },
      testM("returns nothing if the DB is empty") {
        for {
          eventRepository <- liveEventRepository
          foundEvent <- eventRepository.findById(TestData.event.id)
        } yield assert(foundEvent)(isNone)
      },
      testM("returns nothing if no event with the given id exists") {
        for {
          eventRepository <- liveEventRepository
          _ <- userRepository.create(TestData.user)
          _ <- eventRepository.create(TestData.event)
          foundEvent <- eventRepository.findById(otherId)
        } yield assert(foundEvent)(isNone)
      },
    )
  )
}
