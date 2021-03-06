package com.tenzki.akkacrqrs.cqrs

import java.util.UUID

object CommandQueryProtocol {

  sealed trait UserMsg {
    val userId: UUID
  }

  sealed trait UserCmd extends UserMsg

  case class CreateUser(userId: UUID, email: String, firstName: String, lastName: String) extends UserCmd
  case class EditUser(userId: UUID, firstName: String, lastName: String) extends UserCmd

  sealed trait UserAck extends UserMsg

  case class CreatedUserAck(userId: UUID) extends UserAck
  case class UpdatedUserAck(userId: UUID) extends UserAck
  case class InvalidUserAck(userId: UUID, msg: String) extends UserAck

  sealed trait UserQuery extends UserMsg
  sealed trait UserServiceQuery

  case class GetUserDetailsQuery(userId: UUID) extends UserQuery
  case class SearchUsersByFirstName(firstName: String) extends UserServiceQuery

  sealed trait UserQueryResponse extends UserMsg
  sealed trait UserServiceQueryResponse

  case class UserDetailsResponse(userId: UUID, email: String, firstName: String, lastName: String) extends UserQueryResponse
  case class InvalidUserQueryResponse(userId: UUID, message: String) extends UserQueryResponse

  case class ListUser(userId: UUID, email: String, firstName: String, lastName: String)
  case class UserListResponse(users: Seq[ListUser]) extends UserServiceQueryResponse

}
