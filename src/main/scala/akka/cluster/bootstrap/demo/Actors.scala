package akka.cluster.bootstrap.demo

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.bootstrap.demo.EventsReqHandler.ClusterListener
import akka.cluster.typed.{Cluster, Subscribe}

object ActorsRoot {
  def apply(): Behavior[EventsReqHandler.Cmd] = Behaviors.setup { ctx =>
    val watcher = ctx.spawn(EventsReqHandler(), "watcher")

    Cluster(ctx.system).subscriptions ! Subscribe(
      ctx.spawnAnonymous(ClusterListener(watcher)),
      classOf[ClusterDomainEvent]
    )

    Behaviors
      .supervise(Behaviors.receiveMessage[EventsReqHandler.Cmd] { cmd: EventsReqHandler.Cmd =>
        watcher ! cmd
        Behavior.same
      })
      .onFailure(SupervisorStrategy.restart.withStopChildren(false))
  }
}

object EventsReqHandler {

  sealed trait Cmd
  final case class Get(ref: ActorRef[List[ClusterDomainEvent]]) extends Cmd
  private final case class Set(evt: ClusterDomainEvent)         extends Cmd

  object ClusterListener {
    def apply(ref: ActorRef[EventsReqHandler.Cmd]): Behavior[ClusterDomainEvent] =
      Behaviors
        .supervise(Behaviors.receiveMessage[ClusterDomainEvent] { evt: ClusterDomainEvent =>
          ref ! EventsReqHandler.Set(evt)
          Behaviors.same
        })
        .onFailure(SupervisorStrategy.resume)
  }

  def apply(evs: List[ClusterDomainEvent] = Nil): Behavior[Cmd] =
    Behaviors
      .supervise(Behaviors.receive[Cmd] {
        case (ctx, Set(evt)) =>
          ctx.log.info("C_EVT: " + evt)
          apply(evt :: evs)
        case (_, Get(ref)) =>
          ref ! evs
          Behaviors.same
      })
      .onFailure(SupervisorStrategy.resume)
}
