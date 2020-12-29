package eventer.infrastructure

import eventer.domain.event.EventRepository
import eventer.domain.{EventId, TestData}
import zio.test.Assertion._
import zio.test._
import zio.{Has, URIO, ZIO}

import java.util.UUID

object DbEventRepositorySpec extends DbSpec {
  private val otherId = EventId(UUID.fromString("b10c8d00-659e-4d2a-aa3b-0aea3d60c2ca"))

  private val userRepositoryM: URIO[Has[DatabaseContext.Service], DbUserRepository[String]] =
    ZIO.service[DatabaseContext.Service].map(new DbUserRepository[String](_, identity, identity))

  val dbSpec: DbEnvSpec = suite("DbEventRepository")(
    suite("create")(
      testM("inserts the given event into the DB") {
        for {
          userRepository <- userRepositoryM
          _ <- userRepository.create(TestData.user)
          _ <- EventRepository.create(TestData.event)
          foundEvent <- EventRepository.findAll
        } yield assert(foundEvent)(equalTo(Seq(TestData.event)))
      }
    ),
    suite("findAll")(
      testM("returns all events from the DB") {
        for {
          userRepository <- userRepositoryM
          _ <- userRepository.create(TestData.user)
          _ <- EventRepository.create(TestData.event)
          _ <- EventRepository.create(TestData.event.copy(id = otherId))
          foundEvents <- EventRepository.findAll
        } yield assert(foundEvents)(equalTo(Seq(TestData.event, TestData.event.copy(id = otherId))))
      },
      testM("returns nothing if the DB is empty") {
        assertM(EventRepository.findAll)(isEmpty)
      }
    ),
    suite("findById")(
      testM("returns the event with the given id if it exists") {
        for {
          userRepository <- userRepositoryM
          _ <- userRepository.create(TestData.user)
          _ <- EventRepository.create(TestData.event)
          foundEvent <- EventRepository.findById(TestData.event.id)
        } yield assert(foundEvent)(isSome(equalTo(TestData.event)))
      },
      testM("returns nothing if the DB is empty") {
        for {
          foundEvent <- EventRepository.findById(TestData.event.id)
        } yield assert(foundEvent)(isNone)
      },
      testM("returns nothing if no event with the given id exists") {
        for {
          userRepository <- userRepositoryM
          _ <- userRepository.create(TestData.user)
          _ <- EventRepository.create(TestData.event)
          foundEvent <- EventRepository.findById(otherId)
        } yield assert(foundEvent)(isNone)
      },
    )
  ).provideSomeLayer(DbEventRepository.live)
}
