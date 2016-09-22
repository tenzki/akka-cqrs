package com.tenzki.akkacrqrs.cqrs

import java.util.UUID

import akka.actor.{ActorLogging, Actor, Props, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor
import akka.persistence.query.EventEnvelope
import com.tenzki.akkacrqrs.cqrs.CommandQueryProtocol._
import com.tenzki.akkacrqrs.cqrs.UserProcessor.{UserEditEvt, UserCreatedEvt}
import com.tenzki.akkacrqrs.util.{Passivation, Unstash, StartReading, Start}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object UserView {

  def props(userRepo: DBUserRepo):Props = Props(classOf[UserView], userRepo)

  val extractor: ShardRegion.MessageExtractor = new HashCodeMessageExtractor(100) {
    override def entityId(message: Any): String = message match {
      case m : UserQuery => m.userId.toString
      case m : Start => m.persistenceId.split(":")(1)
      case e : EventEnvelope => e.persistenceId.split(":")(1)
    }
  }

  val shardName: String = "UserView"

}

class UserView(userRepo: DBUserRepo) extends Actor with Stash with Passivation with ActorLogging {

  /** passivate the entity when no activity */
  context.setReceiveTimeout(1.minute)

  def receive: Receive = passivate(initial).orElse(unknownCommand)

  def initial: Receive = {
    case Start(persistenceId: String) =>
      val sendr = sender()
      val userId = UUID.fromString(persistenceId.split(":")(1))
      userRepo.findSeqNoById(userId).map {
        case Some(seqNo: Long) => sendr ! StartReading(persistenceId, seqNo)
        case None => sendr ! StartReading(persistenceId, 0)
      }
      init(userId)
    case EventEnvelope(_, persistenceId, _, _) =>
      stash()
      val userId = UUID.fromString(persistenceId.split(":")(1))
      init(userId)
    case query: UserQuery =>
      stash()
      init(query.userId)
  }

  def init(id: UUID): Unit = {
    userRepo.findById(id).onComplete {
      case Success(userO) =>
        userO match {
          case Some(user) =>
            context.become(passivate(active(user)).orElse(unknownCommand))
            self ! Unstash
          case None =>
            context.become(passivate(nonExisting()).orElse(unknownCommand))
            self ! Unstash
        }
      case Failure(t) =>
        println("An error has occurred: " + t.getMessage)
        unstashAll()
    }
    context.become(passivate(configuring()).orElse(unknownCommand))
  }

  def configuring(): Receive = {
    case _ => stash()
  }

  def nonExisting(): Receive = {
    case Unstash => unstashAll()
    case EventEnvelope(_, _, seqNo: Long, e: UserCreatedEvt) =>
      val sendr = sender()
      userRepo.create(e.userId, e.email, e.firstName, e.lastName, e.time, seqNo).onComplete {
        case Success(_) =>
          context.become(passivate(active(new User(e.userId, e.email, e.firstName, e.lastName,  e.time)))
            .orElse(unknownCommand))
          self ! Unstash
          sendr ! CreatedUserAck(e.userId)
        case Failure(t) =>
          println("An error has occurred: " + t.getMessage)
          self ! Unstash
      }
      context.become(passivate(configuring()).orElse(unknownCommand))
    case EventEnvelope => sender() ! InvalidUserQueryResponse(null, "User not existing")
    case q: UserQuery => sender() ! InvalidUserQueryResponse(q.userId, "User not existing")
  }

  def active(user: User): Receive = {
    case Unstash => unstashAll()
    case EventEnvelope(_, _, _, c: UserCreatedEvt) =>
      sender() ! InvalidUserAck(c.userId, "User already existing")
    case EventEnvelope(_, _, seqNo: Long, e: UserEditEvt) =>
      val sendr = sender()
      userRepo.updateById(e.userId, e.firstName, e.lastName, seqNo).onSuccess {
        case _ =>
          sendr ! UpdatedUserAck(e.userId)
          context.become(passivate(active(user.copy(firstName= e.firstName, lastName = e.lastName)))
            .orElse(unknownCommand))
      }
    case GetUserDetailsQuery(userId: UUID) =>
      sender() ! UserDetailsResponse(userId, user.email, user.firstName, user.lastName)
  }

  def unknownCommand: Receive = {
    case other =>
      sender() ! InvalidUserQueryResponse(null, "InvalidUserQueryResponse")
  }

}
