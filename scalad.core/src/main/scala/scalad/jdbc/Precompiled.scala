package scalad.jdbc

/**
 * @author janmachacek
 */
trait Precompiled extends ExecutionPolicy {

  def execute[R <: Result](f: => R): R = f
  
}