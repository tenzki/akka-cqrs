package com.tenzki.akkacrqrs.cqrs

import akka.actor.{Props, Actor}

object UserService {

  def props(userRepo: DBUserRepo):Props = Props(classOf[UserView], userRepo)

  val name: String = "UserService"

}

class UserService(userRepo: DBUserRepo) extends Actor{
  override def receive: Receive = ???
}
