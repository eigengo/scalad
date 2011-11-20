package scalad.jpa

import scalad.{GroupBy, OrderBy, Restriction, Query}


/**
 * @author janmachacek
 */

class JPAQuery(private[scalad] val restriction: Restriction,
             private[scalad] val orderByClauses: List[OrderBy],
             private[scalad] val groupByClauses: List[GroupBy])
  extends Query(restriction, orderByClauses, groupByClauses) {

  def inner(join: Join) = this
  
  def outer(join: Join) = this

}

final class Join(path: String, inner: Boolean, eager: Boolean) {
  
  def fetch = new Join(path, inner, true)
  
}
