package scalad.jdbc

/**
 * @author janmachacek
 */
trait Immediate extends ExecutionPolicy {
  type Result[R] = R

  private[jdbc] def exec[A](f: => A) = f
}