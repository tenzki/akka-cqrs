package com.tenzki.akkacrqrs.cqrs

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.Timeout
import com.tenzki.akkacrqrs.util.{Start, StartReading}

import scala.concurrent.Future
import scala.concurrent.duration._

object ReadCoordinator {

  def props(userView: ActorRef) = Props(classOf[ReadCoordinator], userView)

  def NAME = "read-coordinator"
}

class ReadCoordinator(userView: ActorRef) extends Actor {

  implicit val mat = ActorMaterializer()
  implicit val timeout = Timeout(20.seconds)

  val readJournal = PersistenceQuery(context.system)
    .readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  readJournal.allPersistenceIds()
    .runForeach { persistenceId =>
      if(persistenceId.startsWith("user:")) {
        view(persistenceId).foreach(_ ! Start(persistenceId))
      }
    }

  override def receive: Receive = {
    case StartReading(persistenceId: String, seqNo: Long) =>
      readJournal.eventsByPersistenceId(persistenceId, seqNo + 1, Long.MaxValue).mapAsync(1)(event => {
        view(persistenceId) match {
          case Some(view: ActorRef) =>
            view ? event
          case None => Future.successful(())
        }
      }).runWith(Sink.ignore)
  }

  def view(persistenceId: String): Option[ActorRef] = {
    persistenceId match {
      case u if u.startsWith("user:") => Some(userView)
      case _ => None
    }
  }

}
