package scalad

/**
 * Query that groups the restrictions using the appropriate binary operators
 */
class Query (private[scalad] val restriction: Restriction,
             private[scalad] val orderByClauses: List[OrderBy],
             private[scalad] val groupByClauses: List[GroupBy]) {

  /**
   * Conjoin two queries and return a new {{Query}} with the two queries
   * as its parts
   *
   * @param q the query to be conjoined
   * @return the conjoined query
   */
  def &&(q: Query) = new Query(Conjunction(this, q), orderByClauses, groupByClauses)

  /**
   * Disjoin two queries and return a new {{Query}} with the two queries
   * as its parts
   *
   * @param q the query to be disjoined
   * @return the disjoined query
   */
  def ||(q: Query) = new Query(Disjunction(this, q), orderByClauses, groupByClauses)
  

  def orderBy(o: OrderBy) = new Query(restriction, o :: orderByClauses, groupByClauses)
  
  def orderBy(property: Property) = new Query(restriction, Asc(property) :: orderByClauses, groupByClauses)

  def orderBy(orderBys: OrderBy*) = new Query(restriction, orderBys.toList ::: orderByClauses, groupByClauses)

  def groupBy(groupBys: GroupBy*) = new Query(restriction, orderByClauses, groupBys.toList ::: groupByClauses)
    

  override def toString = {
    val sb = new StringBuilder
    sb.append(restriction.toString)
    if (!orderByClauses.isEmpty) {
      sb.append(" order by ").append(orderByClauses.toString())
    }
    if (!groupByClauses.isEmpty) {
      sb.append(" group by ").append(groupByClauses.toString())
    }

    sb.toString()
  }

}

/**
 * Group by clause regarding the property
 */
final case class GroupBy(property: Property)

class PartialOrder(val property: Property) {
  def asc = Asc(property)
  def desc = Desc(property)
}

/**
 * Order by abstract class with the self-documenting {{Asc}} and {{Desc}} subclasses
 */
abstract class OrderBy
final case class Asc(property: Property) extends OrderBy
final case class Desc(property: Property) extends OrderBy

abstract class Property
final case class NamedProperty(property: String) extends Property
final case class Identity() extends Property

/**
 * Partial restriction allows me to build the complete restriction by completing
 * it with the appropriate operator
 *
 * @param property the property to start the restriction with
 */
class PartialRestriction(val property: Property) {

  /**
   * Alias for ＝ for poor souls who do not have the U.S. Scalaz keyboard
   * layout
   */
  def is = ＝_

  def isNot = !＝_

  def gt = > _

  def lt = < _

  def ＝(value: Any) = Binary(property, '==, value)

  def >(value: Any) = Binary(property, '>, value)
  
  def <(value: Any) = Binary(property, '<, value)
  
  def >=(value: Any) = Binary(property, '>=, value)

  def <=(value: Any) = Binary(property, '>=, value)
  
  def !＝(value: Any) = Binary(property, '!=, value)
  
  def in(value: Any*) = In(property, value)
  
  def isNull = IsNull(property)
  
  def like(value: String) = Like(property, value)

}

/**
 * The base restriction; the case classes represent the concrete restriction cases; the names of the {{Restriction}}
 * subclasses' names should be descriptive enough :)
 */
abstract class Restriction {

  /**
   * Guard-like method that can collapse any restriction down to
   * {{Tautology()}} if {{b}} is not {{true}}.
   * 
   * Typical usage pattern is
   * <pre>
   *   val someValue = 100
   *   val query = (id ＝ 5 when someValue < 100)
   *   
   *   // query is just Tautology()
   * </pre>
   *
   * @param b function that must return {{true}} for the restriction to hold
   */
  def when(b: => Boolean) = {
    if (b) this
    else Tautology()
  }
  
}
final case class Binary(property: Property, op: Symbol, value: Any) extends Restriction
final case class In(property: Property, value: Seq[Any]) extends Restriction
final case class IsNull(property: Property) extends Restriction
final case class Not(restriction: Restriction) extends Restriction
final case class Like(property: Property, value: String) extends Restriction
final case class Conjunction(lhs: Query, rhs: Query) extends Restriction
final case class Disjunction(lhs: Query, rhs: Query) extends Restriction
final case class Tautology() extends Restriction
final case class Contradiction() extends Restriction