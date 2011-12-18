package scalad.jdbc

/**
 * @author janmachacek
 */
trait Immediate extends ExecutionPolicy {

  def execute[R <: Result](f: => R): R = f

}