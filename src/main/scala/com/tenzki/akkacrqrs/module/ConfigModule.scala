package com.tenzki.akkacrqrs.module

import com.google.inject.AbstractModule
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import net.codingwell.scalaguice.ScalaModule

class ConfigModule(akkaHostname: String, akkaPort: Int) extends AbstractModule with ScalaModule {

  val config: Config = ConfigFactory.load()
    .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(akkaHostname))
    .withValue("akka.remote.netty.tcp.port", ConfigValueFactory.fromAnyRef(akkaPort))

  override def configure() {
    bind[Config].toInstance(config)
  }

}