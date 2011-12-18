package scalad.jdbc

/**
 * @author janmachacek
 */

class PrecompiledStatement[T] {

  def apply(template: Any) = {
    println("Taking existing prepared statement and using it with " + template)

    None
  }

  def apply = None

  def close() { }

}