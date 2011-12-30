package scalad.jdbc

/**
 * @author janmachacek
 */
trait ExecutionPolicy {
  type Result[R]

  private[jdbc] def exec[A](f: => A): Result[A]

}