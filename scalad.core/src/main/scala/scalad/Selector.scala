package scalad

/**
 * @author janmachacek
 */
private[scalad] object Selector {
  type Execute[T, Q <: Query] = (Q, PropertyTranslator) => T

}

case class PropertyTranslator (translate: Property => Property) {
  
  def unapply(p: Property): Option[Property] = {
    Some(translate(p))
  }
  
}

class Selector[T, R, Q <: Query] private[scalad](execute: Selector.Execute[R, Q]) {

  def apply(q: Q) = {
    execute(q, PropertyTranslator(p => p))
  }
  
  def apply(f: T => Q)(implicit evidence: ClassManifest[T]) = {
    val e = new Example(evidence.erasure.newInstance().asInstanceOf[T])
    val q = f(e.getExample)
    def translateProperty(p: Property) = p match {
      case NamedProperty(n) => NamedProperty("username")
      case x => x
    }
    execute(q, PropertyTranslator(translateProperty _))
  }
  
  private class Example[T](private val example: T) {
    
    def getExample = {
      
      example
    }
    
    def translate(name: String) = {
      name
    }
  }
  
}