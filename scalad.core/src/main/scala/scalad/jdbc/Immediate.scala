package scalad.jdbc

/**
 * @author janmachacek
 */
trait Immediate extends ExecutionPolicy {
  type Result[R] = R

  def exec[A](a: A) = a
}