package scalad.example.akka

import akka.actor.BootableActorLoaderService
import akka.util.AkkaLoader
import javax.servlet.{ServletContextListener, ServletContextEvent}
import akka.remote.BootableRemoteActorService

/**
 * @author janmachacek
 */
class Initializer extends ServletContextListener {

  lazy val loader = new AkkaLoader

  def contextDestroyed(e: ServletContextEvent) {
    loader.shutdown()
  }

  def contextInitialized(e: ServletContextEvent) {
     loader.boot(false, new BootableActorLoaderService with BootableRemoteActorService )
  }

}