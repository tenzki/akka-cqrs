package com.tenzki.akkacrqrs.module

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.{AbstractModule, Provider}
import com.tenzki.akkacrqrs.module.AkkaModule.ActorProvider
import com.typesafe.config.Config
import net.codingwell.scalaguice.ScalaModule

object AkkaModule {
  class ActorProvider @Inject() (config: Config) extends Provider[ActorSystem] {
    override def get() = ActorSystem("cqrs", config)
  }
}

class AkkaModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[ActorSystem].toProvider[ActorProvider].asEagerSingleton()
  }
}

