package scalad.example.akka

import akka.config.Supervision._
import akka.actor.{Actor, SupervisorFactory}
import akka.http.RootEndpoint

/**
 * @author janmachacek
 */
class Boot {

  val factory = SupervisorFactory(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 100),
      Supervise(Actor.actorOf[RootEndpoint], Permanent) ::
      Supervise(Actor.actorOf[GatekeeperService], Permanent) ::
      Nil))

  factory.newInstance.start

}