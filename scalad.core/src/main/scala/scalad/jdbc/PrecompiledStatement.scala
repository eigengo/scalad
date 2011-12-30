package scalad.jdbc

/**
 * @author janmachacek
 */

class PrecompiledStatement[R](f: => R) {

  def apply(template: Any*)/*: R*/ = {
    println("Taking existing prepared statement and using it with " + template)

    None
  }

  def close() { }

}