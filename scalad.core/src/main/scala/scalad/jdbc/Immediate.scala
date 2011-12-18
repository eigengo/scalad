package scalad.jdbc


/**
 * @author janmachacek
 */

trait Immediate extends ExecutionPolicy {

  def execute[R](f: => R): R = f

}