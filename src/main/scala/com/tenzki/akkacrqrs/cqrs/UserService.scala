package com.tenzki.akkacrqrs.cqrs

import akka.actor.{Actor, Props}
import akka.routing.FromConfig
import com.tenzki.akkacrqrs.cqrs.CommandQueryProtocol.{ListUser, UserListResponse, SearchUsersByFirstName, UserServiceQuery}

object UserService {

  def props():Props = Props(classOf[UserView])

  val name: String = "UserService"

}

class UserService() extends Actor {

  val workerRouter = context.actorOf(FromConfig.props(Props[UserServiceWorker]), name = "WorkerRouter")

  override def receive: Receive = {
    case query: UserServiceQuery => workerRouter.tell(query, sender())
  }

}

object UserServiceWorker {
  def props(userRepo: DBUserRepo):Props = Props(classOf[UserView], userRepo)

  val name: String = "UserServiceWorker"
}

class UserServiceWorker(userRepo: DBUserRepo) extends Actor {

  import akka.pattern.pipe

  override def receive: Actor.Receive = {
    case SearchUsersByFirstName(firstName: String) =>
      implicit val ec = context.dispatcher
      val result = userRepo.searchUsersByFirstName(firstName)
        .map(_.map(user => ListUser(user.id, user.email, user.firstName, user.lastName))).map(UserListResponse)
      pipe(result).to(sender())
  }

}