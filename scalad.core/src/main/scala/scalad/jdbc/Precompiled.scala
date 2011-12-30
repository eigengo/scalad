package scalad.jdbc

/**
 * @author janmachacek
 */
trait Precompiled extends ExecutionPolicy {
  type Result[R] = PrecompiledStatement[R]

  private[jdbc] def exec[A](f: => A) = new PrecompiledStatement[A](f)
}