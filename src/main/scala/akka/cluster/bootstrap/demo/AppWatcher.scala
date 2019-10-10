package akka.cluster.bootstrap.demo

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.typed.{Cluster, Subscribe}

object AppWatcher {

  sealed trait Cmd
  final case class Get(ref: ActorRef[List[ClusterDomainEvent]]) extends Cmd
  final case class Set(evt: ClusterDomainEvent)                 extends Cmd

  object ClusterListener {
    def apply(ref: ActorRef[AppWatcher.Cmd]): Behavior[ClusterDomainEvent] =
      Behaviors.receiveMessage { evt: ClusterDomainEvent =>
        ref ! AppWatcher.Set(evt)
        Behaviors.same
      }
  }

  def apply(): Behavior[Cmd] = Behaviors.setup { ctx =>
    Cluster(ctx.system).subscriptions !
      Subscribe(ctx.spawn(ClusterListener(ctx.self), "listener"), classOf[ClusterDomainEvent])

    def behavior(evs: List[ClusterDomainEvent] = Nil): Behavior[Cmd] = {
      Behaviors.receiveMessage {
        case Set(evt) =>
          ctx.log.info("C_EVT: " + evt)
          behavior(evt :: evs)
        case Get(ref) =>
          ref ! evs
          behavior(evs)
      }
    }
    behavior()
  }
}
