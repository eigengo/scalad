package scalad.jpa

import scalad._


/**
 * @author janmachacek
 */

class JPAQuery(restriction: Restriction,
             orderByClauses: List[OrderBy],
             groupByClauses: List[GroupBy],
             private[jpa] val joins: List[Join])
  extends Query(restriction, orderByClauses, groupByClauses) {

  private def join(join: Join) = new JPAQuery(restriction, orderByClauses, groupByClauses, join :: joins) 
  
  def innerJoin(path: String) = join(Join(path, true, false))
  
  def outerJoin(path: String) = join(Join(path, false, false))
  
  def innerJoinFetch(path: String) = join(Join(path, true, true))
  
  def outerJoinFetch(path: String) = join(Join(path, false, true))

  override def simplify = new JPAQuery(simplifyRestriction(restriction), orderByClauses, groupByClauses, joins)
}

final case class Join(path: String, inner: Boolean, eager: Boolean)