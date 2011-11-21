package scalad


/**
 * Query that groups the restrictions using the appropriate binary operators
 */
class Query (private[scalad] val restriction: Restriction,
             private[scalad] val orderByClauses: List[OrderBy],
             private[scalad] val groupByClauses: List[GroupBy],
             private[scalad] val pageOption: Option[Page]) extends RestrictionSimplifier {

  /**
   * Conjoin two queries and return a new `Query` with the two queries
   * as its parts
   *
   * @param q the query to be conjoined
   * @return the conjoined query
   */
  def &&(q: Query) = new Query(Conjunction(this.restriction, q.restriction), orderByClauses, groupByClauses, pageOption)

  /**
   * Disjoin two queries and return a new `Query` with the two queries
   * as its parts
   *
   * @param q the query to be disjoined
   * @return the disjoined query
   */
  def ||(q: Query) = new Query(Disjunction(this.restriction, q.restriction), orderByClauses, groupByClauses, pageOption)

  /**
   * Add an _order by_ clause to the query
   */
  def orderBy(o: OrderBy) = new Query(restriction, o :: orderByClauses, groupByClauses, pageOption)

  /**
   * Add an _order by asc `property`_ clause to the query
   */
  def orderBy(property: Property) = new Query(restriction, Asc(property) :: orderByClauses, groupByClauses, pageOption)

  /**
   * Add many _order by_ clauses to the query
   */
  def orderBy(orderBys: OrderBy*) = new Query(restriction, orderBys.toList ::: orderByClauses, groupByClauses, pageOption)

  /**
   * Add many _group by_ clauses to the query
   */
  def groupBy(groupBys: GroupBy*) = new Query(restriction, orderByClauses, groupBys.toList ::: groupByClauses, pageOption)

  /**
   * Specify paging
   */
  def page(page: Page) = new Query(restriction, orderByClauses, groupByClauses, Some(page))

  /**
   * Return simplified query
   */
  def simplify = new Query(simplifyRestriction(restriction), orderByClauses, groupByClauses, pageOption)

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
 * Paging definition
 */
final case class Page(firstRow: Int, maximumRows: Int)

/**
 * Group by clause regarding the property
 */
final case class GroupBy(property: Property)

class PartialOrder(val property: Property) {
  def asc = Asc(property)
  def desc = Desc(property)
}

/**
 * Order by abstract class with the self-documenting `Asc` and `Desc` subclasses
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
  
  def isIn(value: Any*) = In(property, value)
  
  def isNull = IsNull(property)
  
  def like(value: String) = Like(property, value)

}

/**
 * The base restriction; the case classes represent the concrete restriction cases; the names of the `Restriction`
 * subclasses' names should be descriptive enough :)
 */
abstract class Restriction {

  /**
   * Guard-like method that can collapse any restriction down to
   * `Nothing()` if `b` is not `true`.
   * 
   * Typical usage pattern is
   * <pre>
   *   val someValue = 100
   *   val query = (id ＝ 5 when someValue < 100)
   *   
   *   // query is just Nothing()
   * </pre>
   *
   * @param b function that must return `true` for the restriction to hold
   */
  def when(b: => Boolean) = {
    if (b) this
    else Nothing()
  }
  
}

/**
 * Binary restriction in form of `property` `op` `value`, for example
 * `NamedProperty(foo) '== a`, which should translate to (in ANSI SQL-speak here)
 * `where foo = "a"`.
 * <br/>
 * The `op` can be one of:
 * <ul>
 *   <li>`'==`: exactly equals</li>
 *   <li>`'!=`: exactly not equals</li>
 *   <li>`'!=`: exactly not equals</li>
 *   <li>`'>`: greater than</li>
 *   <li>`'<`: less than</li>
 *   <li>`'>=`: greater than or equals</li>
 *   <li>`'<=`: less than or equals</li>
 * </ul>
 */
final case class Binary(property: Property, op: Symbol, value: Any) extends Restriction

/**
 * Set restriction in form of `property` in `value`, where `value` is some collection of values
 */
final case class In(property: Property, value: Seq[Any]) extends Restriction

/**
 * Is NULL restriction
 */
final case class IsNull(property: Property) extends Restriction
/**
 * Is not NULL restriction
 */
final case class IsNotNull(property: Property) extends Restriction
/**
 * Unary NOT operation; typically, you will use the `!=`, `IsNull` or `IsNotNull` restrictions
 */
final case class Not(restriction: Restriction) extends Restriction
/**
 * String like restriction. The `value` should include the wildcard character, but it is not enforced
 */
final case class Like(property: Property, value: String) extends Restriction
/**
 * Conjunction of two restrictions: `lhs` AND `rhs`
 */
final case class Conjunction(lhs: Restriction, rhs: Restriction) extends Restriction
/**
 * Disjunction of two restrictions: `lhs` OR `rhs`
 */
final case class Disjunction(lhs: Restriction, rhs: Restriction) extends Restriction
/**
 * Always true restriction.
 */
final case class Tautology() extends Restriction
/**
 * Always false restriction.
 */
final case class Contradiction() extends Restriction
/**
 * A 'no-op' restriction; returned by the `Restriction.when` guard when the condition is `false`.
 */
final case class Nothing() extends Restriction
