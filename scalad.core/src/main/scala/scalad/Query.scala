package scalad

/**
 * Query that groups the restrictions using the appropriate binary operators
 */
class Query (val restriction: Restriction) {
  
  def &&(q: Query) = new Query(Conjunction(this, q))

  def ||(q: Query) = new Query(Disjunction(this, q))
    
  override def toString = restriction.toString

}

/**
 * Partial restriction allows me to build the complete restriction by completing
 * it with the appropriate operator
 *
 * @param property the property to start the restriction with
 */
class PartialRestriction(val property: String) {
  
  def ＝(value: Any) = Binary(property, '==, value)

  def >(value: Any) = Binary(property, '>, value)

  def like(value: String) = Like(property, value)
  

}

/**
 * The base restriction; the case classes represent the concrete restriction cases; the names of the {{Restriction}}
 * subclasses' names should be descriptive enough :)
 */
abstract class Restriction
final case class Binary(property: String, op: Symbol, value: Any) extends Restriction
final case class Not(restriction: Restriction) extends Restriction
final case class Like(property: String, value: String) extends Restriction
final case class Conjunction(lhs: Query, rhs: Query) extends Restriction
final case class Disjunction(lhs: Query, rhs: Query) extends Restriction
