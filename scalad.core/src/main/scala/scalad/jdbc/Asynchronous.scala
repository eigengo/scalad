package scalad.jdbc

import java.util.UUID
import java.util.concurrent._

/**
 * @author janmachacek
 */
trait Asynchronous extends ExecutionPolicy {
  type Result[R] = AsynchronousResult[R]

  def begin: UUID = {
    UUID.randomUUID()
  }
  
  private[jdbc] def exec[A](f: => A) = new AsynchronousResult[A](f)
  
  def waitFor(group: UUID, timeout: Long) {
    // do nothing
  }

  class AsynchronousResult[A](private[this] val f: => A) {

    def ? = f

    def apply() = f

  }
}