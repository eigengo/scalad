package scalad.example.akka

import akka.actor.Actor
import akka.http.{RootEndpoint, Endpoint, Get, RequestMethod}
import util.Random

/**
 * @author janmachacek
 */
class GatekeeperService extends Actor with Endpoint {
  self.dispatcher = Endpoint.Dispatcher

  def hook(uri: String) = true

  def provide(uri: String) = {
    Actor.actorOf[GatekeeperActor].start
  }

  override def preStart = {
    Actor.registry.actorFor[RootEndpoint].get ! Endpoint.Attach(hook, provide)
  }

  def receive = handleHttpRequest
}
