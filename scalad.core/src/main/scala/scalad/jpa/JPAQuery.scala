package scalad.jpa

import scalad._

/**
 * @author janmachacek
 */
class JPAQuery(restriction: Restriction,
             orderByClauses: List[OrderBy],
             groupByClauses: List[GroupBy],
             pageOption: Option[Page],
             private[jpa] val joins: List[Join])

  extends Query(restriction, orderByClauses, groupByClauses, pageOption) {

  private def join(join: Join) = new JPAQuery(restriction, orderByClauses, groupByClauses, pageOption, join :: joins)
  
  def innerJoin(path: String) = join(Join(path, true, false))
  
  def outerJoin(path: String) = join(Join(path, false, false))
  
  def innerJoinFetch(path: String) = join(Join(path, true, true))
  
  def outerJoinFetch(path: String) = join(Join(path, false, true))

  override def simplify = new JPAQuery(simplifyRestriction(restriction), orderByClauses, groupByClauses, pageOption, joins)

  override def page(range: Range) = new JPAQuery(restriction, orderByClauses, groupByClauses, Some(Page(range.start, range.end)), joins)
}

final case class Join(path: String, inner: Boolean, eager: Boolean)