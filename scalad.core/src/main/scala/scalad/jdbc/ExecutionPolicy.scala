package scalad.jdbc

/**
 * @author janmachacek
 */
trait ExecutionPolicy {
  type Result[R]

  def exec[A](a: A): Result[A]

}