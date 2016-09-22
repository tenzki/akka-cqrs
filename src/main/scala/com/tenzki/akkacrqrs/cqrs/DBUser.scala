package com.tenzki.akkacrqrs.cqrs

import java.util.UUID
import javax.inject.Inject

import com.tenzki.akkacrqrs.util.DatabaseProvider
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class DBUser(id: UUID, email: String, firstName: String, lastName: String, created: DateTime, seqNo: Long)

class DBUserRepo @Inject() (val dbProvider: DatabaseProvider) {

  import com.github.tototoshi.slick.PostgresJodaSupport._
  import dbProvider.driver.api._

  private val users = TableQuery[DBUsersTable]

  def findSeqNoById(id: UUID): Future[Option[Long]] =
    dbProvider.db.run(users.filter(_.id === id).map(_.seqNo).result.headOption)

  def findById(id: UUID): Future[Option[User]] = dbProvider.db.run(users.filter(_.id === id).result.headOption)
    .map(dbUserO => dbUserO.map(dbUser => new User(dbUser.id, dbUser.email, dbUser.firstName, dbUser.lastName, dbUser.created)))

  def create(id: UUID, email: String, firstName: String, lastName: String, created: DateTime, seqNo: Long): Future[UUID] =
    dbProvider.db.run((users returning users.map(_.id)) += DBUser(id, email, firstName, lastName, created, seqNo))

  def updateById(id: UUID, firstName: String, lastName: String, seqNo: Long): Future[Int] =
    dbProvider.db.run(users.filter(_.id === id).map(u => (u.firstName, u.lastName, u.seqNo))
      .update(firstName, lastName, seqNo))

  private class DBUsersTable(tag: Tag) extends Table[DBUser](tag, "users") {

    def id = column[UUID]("id", O.PrimaryKey)
    def email = column[String]("email")
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def created = column[DateTime]("created")
    def seqNo = column[Long]("seq_number")

    def * = (id, email, firstName, lastName, created, seqNo) <>(DBUser.tupled, DBUser.unapply)
    def ? = (id.?, email.?, firstName.?, lastName.?, created.?, seqNo.?).shaped.<>({ r => import r._; _1.map(_ => DBUser.tupled((_1.get, _2.get, _3.get, _4.get, _5.get, _6.get))) }, (_: Any) => throw new Exception("Inserting into ? projection not supported."))
  }

}