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
   * Add an ''order by'' clause to the query
   *
   * @param o the `OrderBy` clause to be added
   * @return the query with the order by clause included
   */
  def orderBy(o: OrderBy) = new Query(restriction, o :: orderByClauses, groupByClauses, pageOption)

  /**
   * Add an ''order by asc `property`'' clause to the query
   *
   * @param property the `Property` that will become `OrderBy(property, Asc())`
   * @return the query with the order by clause included
   */
  def orderBy(property: Property) = new Query(restriction, Asc(property) :: orderByClauses, groupByClauses, pageOption)

  /**
   * Add many ''order by'' clauses to the query
   *
   * @param orderBys the `OrderBy` clauses to be added
   * @return the query with the order by clauses included
   */
  def orderBy(orderBys: OrderBy*) = new Query(restriction, orderBys.toList ::: orderByClauses, groupByClauses, pageOption)

  /**
   * Add many ''group by'' clauses to the query
   *
   * @param groupBys the `GroupBy` clause to be added
   * @return the query with the group by clause included
   */
  def groupBy(groupBys: GroupBy*) = new Query(restriction, orderByClauses, groupBys.toList ::: groupByClauses, pageOption)

  /**
   * Specify paging
   *
   * @param page the page to be set
   * @return the query with the paging clause included
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

/**
 * Property representing either a named property (indeed a property or a column in the database) or the
 * identity of the row/object.
 */
abstract class Property

/**
 * Named reference to a property or column
 */
final case class NamedProperty(property: String) extends Property

/**
 * The identity property or column
 */
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

  /**
   * Alias for !＝ for poor souls who do not have the U.S. Scalaz keyboard
   * layout
   */
  def isNot = !＝_

  /**
   * Equality restriction: `property` `=` `value`
   *
   * @param value the value to be compared
   * @return `Binary` restriction
   */
  def ＝(value: Any) = Binary(property, '==, value)

  /**
   * Greater than restriction: `property` `>` `value`
   *
   * @param value the value to be compared
   * @return `Binary` restriction
   */
  def >(value: Any) = Binary(property, '>, value)
  
  /**
   * Smaller than restriction: `property` `<` `value`
   *
   * @param value the value to be compared
   * @return `Binary` restriction
   */
  def <(value: Any) = Binary(property, '<, value)
  
  /**
   * Greater than or equal to restriction: `property` `≥` `value`
   *
   * @param value the value to be compared
   * @return `Binary` restriction
   */
  def >=(value: Any) = Binary(property, '>=, value)

  /**
   * Greater than or equal to restriction: `property` `≤` `value`
   *
   * @param value the value to be compared
   * @return `Binary` restriction
   */
  def <=(value: Any) = Binary(property, '>=, value)
  
  /**
   * Not equal to restriction: `property` `≠` `value`
   *
   * @param value the value to be compared
   * @return `Binary` restriction
   */
  def !＝(value: Any) = Binary(property, '!=, value)
  
  /**
   * In restriction: `property` `in` (`value(1)`, `value(2)`, ..., `value(n)`)
   *
   * @param value the values to be matched
   * @return `In` restriction
   */
  def isIn(value: Any*) = In(property, value)
  
  /**
   * Is null restriction `property` `is null`
   *
   * @param value the value to be compared
   * @return `IsNull` restriction
   */
  def isNull = IsNull(property)
  
  /**
   * Like restriction: `property` `like` `value`
   *
   * @param value the value to be compared; typically, the value will include the wildcard character
   *  (typically `%`), but it is not enforced
   * @return `Like` restriction
   */
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
