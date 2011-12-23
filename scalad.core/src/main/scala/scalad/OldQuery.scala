package scalad

/**
 * Query that groups the restrictions using the appropriate binary operators
 */
class OldQuery (private[scalad] val restriction_ : Restriction,
             private[scalad] val orderByClauses: List[OrderBy],
             private[scalad] val groupByClauses: List[GroupBy],
             private[scalad] val pageOption: Option[Page]) extends Restricted {

  
  def restriction(implicit simplifier: RestrictionSimplifier) = simplifier.simplifyRestriction(restriction_)
  
  /**
   * Add an ''order by'' clause to the query
   *
   * @param o the `OrderBy` clause to be added
   * @return the query with the order by clause included
   */
  def orderBy(o: OrderBy) = new OldQuery(restriction_, o :: orderByClauses, groupByClauses, pageOption)

  /**
   * Add an ''order by asc `property`'' clause to the query
   *
   * @param property the `Property` that will become `OrderBy(property, Asc())`
   * @return the query with the order by clause included
   */
  def orderBy(property: Property) = new OldQuery(restriction_, Asc(property) :: orderByClauses, groupByClauses, pageOption)

  /**
   * Add many ''order by'' clauses to the query
   *
   * @param orderBys the `OrderBy` clauses to be added
   * @return the query with the order by clauses included
   */
  def orderBy(orderBys: OrderBy*) = new OldQuery(restriction_, orderBys.toList ::: orderByClauses, groupByClauses, pageOption)

  /**
   * Add many ''group by'' clauses to the query
   *
   * @param groupBys the `GroupBy` clause to be added
   * @return the query with the group by clause included
   */
  def groupBy(groupBys: GroupBy*) = new OldQuery(restriction_, orderByClauses, groupBys.toList ::: groupByClauses, pageOption)

  /**
   * Specify paging
   *
   * @param range the page to be set
   * @return the query with the paging clause included
   */
  def page(range: Range) = new OldQuery(restriction_, orderByClauses, groupByClauses, Some(Page(range.start, range.end)))

  override def toString = {
    val sb = new StringBuilder
    sb.append(restriction_.toString)
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