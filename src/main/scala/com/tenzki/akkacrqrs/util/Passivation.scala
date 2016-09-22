package com.tenzki.akkacrqrs.util

import akka.actor.{ActorLogging, Actor, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion.Passivate

trait Passivation extends ActorLogging {
  this: Actor =>

  protected def passivate(receive: Receive): Receive = receive.orElse{
    // tell parent actor to send us a PoisonPill
    case ReceiveTimeout =>
      log.info("{} ReceiveTimeout: passivating.", self)
      context.parent ! Passivate(stopMessage = Stop)
    // stop
    case Stop =>
      log.info("{} Stop", self)
      context.stop(self)
  }
}
