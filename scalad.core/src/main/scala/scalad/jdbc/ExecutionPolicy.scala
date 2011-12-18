package scalad.jdbc

/**
 * @author janmachacek
 */

trait ExecutionPolicy {
  type Result

  def execute[R <: Result](f: => R): Result

}