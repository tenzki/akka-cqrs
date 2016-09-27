package com.tenzki.akkacrqrs

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import com.tenzki.akkacrqrs.cqrs.CommandQueryProtocol._
import com.typesafe.scalalogging.LazyLogging
import de.heikoseeberger.akkahttpcirce.CirceSupport
import org.joda.time.format.ISODateTimeFormat

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// entities
final case class CreateUserDto(email: String, firstName: String, lastName: String)
final case class EditUserDto(firstName: String, lastName: String)

// responses

final case class UserError(userId: UUID, msg: String)
final case class UserSearchError(msg: String)
final case class UserSuccess(userId: UUID)
final case class UserDetails(userId: UUID, email: String, firstName: String, lastName: String)

trait HttpRoutes extends LazyLogging {

  import CirceSupport._
  import io.circe.generic.auto._

  implicit val ec: ExecutionContext

  implicit val timeout = Timeout(15.seconds)

  lazy val parser = ISODateTimeFormat.dateOptionalTimeParser()

  def route(userCommand: ActorRef, userQuery: ActorRef, userService: ActorRef): Route = {
    pathSingleSlash {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Akka CQRS example</h1>"))
      }
    } ~
    pathPrefix("users") {
      (get & path("search")) {
        parameter('firstName) { firstName =>
        val search = (userService ? SearchUsersByFirstName(firstName)).mapTo[UserListResponse]
        onComplete(search) {
          case Success(s) => s match {
              case UserListResponse(users: Seq[ListUser]) =>
                complete(users)
              case _ =>
                complete(UserSearchError("Unknown response"))
            }
            case Failure(t) =>
              logger.error("GetUserDetailsQuery error: ", t.getMessage)
              complete(UserSearchError(t.getMessage))
        }
        }
      }
      (get & path(JavaUUID)) { id =>
        val get = (userQuery ? GetUserDetailsQuery(id)).mapTo[UserQueryResponse]
        onComplete(get) {
          case Success(s) => s match {
              case UserDetailsResponse(userId: UUID, email: String, firstName: String, lastName: String) =>
                complete(UserDetails(userId, email, firstName, lastName))
              case _ =>
                complete(UserError(id, "Unknown response"))
            }
            case Failure(t) =>
              logger.error("GetUserDetailsQuery error: ", t.getMessage)
              complete(UserError(id, t.getMessage))
        }
      } ~
      (post & entity(as[CreateUserDto])) { createUser =>
        val create = (userCommand ? CreateUser(UUID.randomUUID(), createUser.email, createUser.firstName, createUser.lastName)).mapTo[UserAck]
        onComplete(create){
          case Success(r) => r match {
            case CreatedUserAck(userId: UUID) =>
              complete(UserSuccess(userId))
            case _ =>
              complete(UserError(null, "Unknown response"))
          }
          case Failure(t) =>
            logger.error("CreateUser error: ", t.getMessage)
            complete(UserError(null, t.getMessage))
        }
      } ~
      (put & path(JavaUUID) & entity(as[EditUserDto])) { (id, editUser) =>
        val update = (userCommand ? EditUser(id, editUser.firstName, editUser.lastName)).mapTo[UserAck]
        onComplete(update) {
          case Success(r) => r match {
            case UpdatedUserAck(userId: UUID) =>
              complete(UserSuccess(userId))
            case _ =>
              complete(UserError(null, "Unknown response"))
          }
          case Failure(t) =>
            logger.error("CreateUser error: ", t.getMessage)
            complete(UserError(null, t.getMessage))
        }
      }
    }
  }

}
