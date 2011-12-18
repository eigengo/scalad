package scalad.jdbc

/**
 * @author janmachacek
 */

trait ExecutionPolicy {

  def execute[R](f: => R): R

}