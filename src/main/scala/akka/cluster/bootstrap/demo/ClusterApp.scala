package akka.cluster.bootstrap.demo

import java.util

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.{Cluster, ClusterEvent, MemberStatus}
import akka.http.scaladsl._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer

import scala.util.Try
import akka.pattern._
import akka.util.Timeout

import concurrent.duration._

object ClusterApp extends App {

  implicit val system           = ActorSystem("demo")
  implicit val materializer     = ActorMaterializer()
  implicit val timeout: Timeout = 2.seconds
  import system.dispatcher

  system.log.info(
    "Started [" + system + "], cluster.selfAddress = " + Cluster(system).selfAddress + ")"
  )

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  val watcher: ActorRef = system.actorOf(Props[ClusterWatcher])

  Cluster(system).subscribe(
    watcher,
    ClusterEvent.initialStateAsEvents,
    classOf[ClusterEvent.ClusterDomainEvent]
  )

  val readyStates = new util.HashSet[MemberStatus]
  readyStates.add(MemberStatus.up)
  readyStates.add(MemberStatus.weaklyUp)

  val notReadyErr: Try[MemberStatus] => Throwable = status => new RuntimeException("NOT READY STATUS " + status)

  val route = {
    get {
      path("ready") {
        val status = Try(Cluster(system).selfMember.status)
        if (status.filter(readyStates.contains).isSuccess) {
          complete(StatusCodes.OK, "Status: " + status)
        } else {
          complete(
            StatusCodes.InternalServerError,
            status.toEither.swap
              .getOrElse(notReadyErr(status))
              .getLocalizedMessage
          )
        }
      } ~ path("alive") {
        complete(StatusCodes.OK)
      } ~ path("hello") {
        complete("<h1>Say hello to akka-http</h1>")
      } ~ path("events") {
        complete {
          (watcher ? "get")
            .mapTo[List[ClusterDomainEvent]]
            .map(_.mkString("\n"))
        }
      }
    }
  }

  Http().bindAndHandle(route, "0.0.0.0", 8080)
}
