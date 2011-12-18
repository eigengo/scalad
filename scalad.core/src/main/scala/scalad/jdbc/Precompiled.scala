package scalad.jdbc

/**
 * @author janmachacek
 */
trait Precompiled extends ExecutionPolicy {
  type Result[R] = PrecompiledStatement[R]

  def exec[A](a: A) = new PrecompiledStatement[A](a)
}