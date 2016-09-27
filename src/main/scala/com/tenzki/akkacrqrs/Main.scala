package com.tenzki.akkacrqrs

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.google.inject.Guice
import com.tenzki.akkacrqrs.cqrs._
import com.tenzki.akkacrqrs.module.{AkkaModule, ConfigModule}

import scala.concurrent.duration._
import scala.io.StdIn

class Main extends App with HttpRoutes {

  // configure

  private val argumentsError = """
   Please run the service with the required arguments: " <httpIpAddress>" <httpPort> "<akkaHostIpAddress>" <akkaport> """

  assert(args.length == 4, argumentsError)

  val httpHost = args(0)
  val httpPort = args(1).toInt
  val akkaHostname = args(2)
  val akkaPort = args(3).toInt

  val injector = Guice.createInjector(
    new ConfigModule(akkaHostname, akkaPort),
    new AkkaModule
  )

  // run
  implicit val system = injector.getInstance(classOf[ActorSystem])
  val userRepo = injector.getInstance(classOf[DBUserRepo])
  implicit val materializer = ActorMaterializer()

  implicit val ec = system.dispatcher

  val userView = ClusterSharding(system).start(UserView.shardName, UserView.props(userRepo),
    ClusterShardingSettings(system), UserView.extractor)

  val userService = system.actorOf(UserService.props(), UserService.name)
  system.actorOf(UserServiceWorker.props(userRepo), UserServiceWorker.name)

  val userProcessor = ClusterSharding(system).start(UserProcessor.shardName, UserProcessor.props(),
    ClusterShardingSettings(system), UserProcessor.extractor)


  override implicit val timeout = Timeout(15.seconds)

  val bindingFuture = Http().bindAndHandle(route(userProcessor, userView, userService), interface = httpHost, port = httpPort)
  println(s"Server online at http://$httpHost:$httpPort/\nPress RETURN to stop...")
  StdIn.readLine()
  bindingFuture
      .flatMap(_.unbind())
      .onComplete(_ => system.terminate())

}
