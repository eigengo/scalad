package scalad.jdbc

/**
 * @author janmachacek
 */
trait Precompiled extends ExecutionPolicy {

  def execute[R](f: => R): R = f
  
}