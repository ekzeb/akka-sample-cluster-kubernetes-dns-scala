package akka.cluster.bootstrap.demo

import akka.{Done, actor}
import akka.actor.CoordinatedShutdown
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.MemberStatus
import akka.cluster.typed._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.util.Timeout

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object ClusterApp extends App {
  implicit val actorsRoot: ActorSystem[EventsReqHandler.Cmd] = ActorSystem(ActorsRoot(), "demo")
  implicit val scheduler                                     = actorsRoot.toClassic.scheduler

  implicit val timeout: Timeout = 3.seconds
  import actorsRoot.executionContext

  AkkaManagement(actorsRoot.toClassic).start()
  ClusterBootstrap(actorsRoot.toClassic).start()

  val readyStates = Set[MemberStatus](MemberStatus.up, MemberStatus.weaklyUp)
  // format: off
  val notReadyErr = { status: Try[MemberStatus] => new RuntimeException("NOT READY STATUS " + status) }
  val eventsReq   = { ref: ActorRef[List[ClusterDomainEvent]] => EventsReqHandler.Get(ref) }
  // format: on
  val cluster = Cluster(actorsRoot)

  val route: Route = get {
    path("ready") {
      Try {
        val status = cluster.selfMember.status
        readyStates(status) -> status
      } match {
        case Success((ready, status)) =>
          if (ready) complete(StatusCodes.OK, "Status: " + status)
          else complete(StatusCodes.ServiceUnavailable, "Status: " + status)
        case Failure(err) =>
          complete(StatusCodes.InternalServerError, s"Akka cluster Node not ready: ${err.getLocalizedMessage}")
      }
    } ~ path("alive") {
      complete(StatusCodes.OK)
    } ~ path("hello") {
      complete("<h1>Say hello to akka-http</h1>")
    } ~ path("events") {
      complete {
        actorsRoot.ask { eventsReq } map { _.mkString("\n") }
      }
    }
  }

  Server.start(route, actorsRoot)
}

object Server {
  def start(route: Route, system: ActorSystem[_]) = {
    import akka.actor.typed.scaladsl.adapter._
    implicit val classicSystem: actor.ActorSystem = system.toClassic
    val shutdown                                  = CoordinatedShutdown(classicSystem)
    import system.executionContext

    Http().bindAndHandle(route, "0.0.0.0", 8080).onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Shopping online at http://{}:{}/", address.getHostString, address.getPort)

        shutdown.addTask(CoordinatedShutdown.PhaseServiceRequestsDone, "http-graceful-terminate") { () =>
          binding.terminate(10.seconds).map { _ =>
            system.log
              .info("Shopping http://{}:{}/ graceful shutdown completed", address.getHostString, address.getPort)
            Done
          }
        }
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }
}
