package akka.cluster.bootstrap.demo

import akka.actor.{Actor, ActorLogging}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.ClusterDomainEvent

class ClusterWatcher extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def receive: Receive =
    handleCLusterEvents(Nil)

  def handleCLusterEvents(ex: List[ClusterDomainEvent]): Receive = {
    case evt: ClusterDomainEvent =>
      log.info("!>>> Cluster " + cluster.selfAddress + " >>> " + evt)
      context.become(handleCLusterEvents(evt :: ex))
    case "get" =>
      sender() ! ex
  }
}
