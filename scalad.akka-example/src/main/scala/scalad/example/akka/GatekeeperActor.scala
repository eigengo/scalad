package scalad.example.akka

import akka.actor.Actor
import akka.http.{Get, RequestMethod}
import util.Random

/**
 * @author janmachacek
 */
class GatekeeperActor extends Actor {
  private val random = new Random

  def receive = {
    case get: Get =>
      get OK (if (random.nextInt() % 2 == 0) "G" else "S")
    case other: RequestMethod =>
      other OK "S"
  }
}
