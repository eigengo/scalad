package scalad.example.akka

import akka.config.Supervision._
import akka.actor.{Actor, SupervisorFactory}

/**
 * @author janmachacek
 */
class Boot {

  val factory = SupervisorFactory(
    SupervisorConfig(
      AllForOneStrategy(List(classOf[Exception])),
      Supervise(Actor.actorOf[GatekeeperService], Permanent) :: Nil))


  factory.newInstance.start

}