package com.tenzki.akkacrqrs.cqrs

import java.util.UUID

import akka.actor.{ActorLogging, Props}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor
import akka.persistence.{PersistentActor, RecoveryCompleted, SnapshotOffer}
import com.tenzki.akkacrqrs.cqrs.CommandQueryProtocol._
import com.tenzki.akkacrqrs.util.Passivation
import org.joda.time.DateTime

import scala.concurrent.duration._

object UserProcessor {

  def props(): Props = Props(classOf[UserProcessor])

  sealed trait UserEvt {
    val userId: UUID
  }

  case class UserCreatedEvt(userId: UUID, email: String, firstName: String, lastName: String, time: DateTime) extends UserEvt

  case class UserEditEvt(userId: UUID, firstName: String, lastName: String, time: DateTime) extends UserEvt

  val extractor: ShardRegion.MessageExtractor = new HashCodeMessageExtractor(100) {
    override def entityId(message: Any): String = message match {
      case m: UserCmd => m.userId.toString
    }
  }

  val shardName: String = "UserProcessor"

}

class UserProcessor() extends PersistentActor with Passivation with ActorLogging {

  import UserProcessor._

  override def persistenceId: String = s"user:${self.path.name}"

  /** passivate the entity when no activity for 1 minute */
  context.setReceiveTimeout(1.minute)

  override def receiveCommand: Receive = passivate(initial).orElse(unknownCommand)

  def initial: Receive = {
    case CreateUser(userId: UUID, email: String, firstName: String, lastName: String) =>
      val sendr = sender()
      persist(new UserCreatedEvt(userId, email, firstName, lastName, DateTime.now())) { evt =>
        sendr ! new CreatedUserAck(userId)
        context.become(passivate(existing(new User(userId, email, firstName, lastName, evt.time))).orElse(unknownCommand))
      }
  }

  def existing(user: User): Receive = {
    case EditUser(userId: UUID, firstName: String, lastName: String) =>
      val sendr = sender()
      persist(new UserEditEvt(userId, firstName, lastName, DateTime.now())) { evt =>
        sendr ! UpdatedUserAck(userId)
        context.become(passivate(existing(user.copy(firstName = firstName, lastName = lastName))).orElse(unknownCommand))
      }
  }

  private var recoverStateMaybe: Option[User] = None

  override def receiveRecover: Receive = {
    case event: UserCreatedEvt =>
      log.info("receiveRecover event: {}", event)
      recoverStateMaybe = Some(User(event.userId, event.email, event.firstName, event.lastName, event.time))
    case event: UserEditEvt =>
      recoverStateMaybe = recoverStateMaybe.map(state =>
        state.copy(firstName = event.firstName, lastName = event.lastName))
    case RecoveryCompleted => postRecoveryBecome(recoverStateMaybe)
    case SnapshotOffer(_, snapshot) =>
      val maybeUser: Option[User] = snapshot.asInstanceOf[Option[User]]
      log.info("recovery from snapshot state: {}", maybeUser)
      postRecoveryBecome(maybeUser)
  }

  def postRecoveryBecome(auctionRecoverStateMaybe: Option[User]): Unit =
    auctionRecoverStateMaybe.fold[Unit]({}) { user =>
      log.info("postRecoveryBecome")
      context.become(passivate(existing(user)).orElse(unknownCommand))
    }

  def unknownCommand: Receive = {
    case other =>
      log.info("unknown command {}", other)
      sender() ! InvalidUserAck(null, "InvalidUserAck")
  }

}
