package scalad.jdbc

import java.util.UUID
import java.util.concurrent._
import java.lang.Thread

/**
 * @author janmachacek
 */
trait Asynchronous extends ExecutionPolicy {
  type Result[R] = AsynchronousResult[R]
  val executor = Executors.newFixedThreadPool(20)

  def begin: UUID = {
    UUID.randomUUID()
  }
  
  private[jdbc] def exec[A](f: => A) = {
    new AsynchronousResult[A](f)
  }
  
  def waitFor(group: UUID, timeout: Long) {
    // do nothing
  }

  class AsynchronousResult[A](private[this] val f: => A) {
    val future = new FutureTask[A](new Callable[A] {
      def call() = f
    })

    executor.execute(future)

    def ? = future.get

    def apply() = future.get

  }
}