package scalad.example.akka

import akka.actor.Actor
import javax.ws.rs.{GET, Produces, Path}

/**
 * @author janmachacek
 */
@Path("/hello")
class GatekeeperService extends Actor {

  private case object Hello

  @GET
  @Produces(Array("text/html"))
  def hello = (self ? Hello).as[String].getOrElse("couldn't say hello")

  def receive = {
    case Hello => self reply(<h1>Hello, World</h1>.toString)
  }

}