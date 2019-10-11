package akka.cluster.bootstrap.demo

import akka.actor.typed._
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.scaladsl.AskPattern._
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.MemberStatus
import akka.cluster.typed._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer
import akka.util.Timeout

import concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}

object ClusterApp extends App {
  val actorsRoot: ActorSystem[EventsReqHandler.Cmd] = ActorSystem(ActorsRoot(), "demo")
  implicit val untypedSystem                        = actorsRoot.toUntyped
  implicit val materializer                         = ActorMaterializer()
  implicit val scheduler                            = untypedSystem.scheduler
  implicit val timeout: Timeout                     = 3.seconds
  import untypedSystem.dispatcher

  AkkaManagement(untypedSystem).start()
  ClusterBootstrap(untypedSystem).start()

  val readyStates = Set[MemberStatus](MemberStatus.up, MemberStatus.weaklyUp)
  val notReadyErr = { status: Try[MemberStatus] =>
    new RuntimeException("NOT READY STATUS " + status)
  }
  val eventsReq = { ref: ActorRef[List[ClusterDomainEvent]] =>
    EventsReqHandler.Get(ref)
  }

  val cluster = Cluster(actorsRoot)

  val route = get {
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

  Http().bindAndHandle(route, "0.0.0.0", 8080)
}
