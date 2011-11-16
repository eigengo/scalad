package scalad.example.akka

import akka.actor.BootableActorLoaderService
import akka.util.AkkaLoader
import javax.servlet.ServletContextEvent

/**
 * @author janmachacek
 */

class Initializer {
  lazy val loader = new AkkaLoader
  def contextDestroyed(e: ServletContextEvent): Unit = loader.shutdown
  def contextInitialized(e: ServletContextEvent): Unit =
//    loader.boot(true, new BootableActorLoaderService with BootableRemoteActorService) //<--- Important
     loader.boot(true, new BootableActorLoaderService {})

}