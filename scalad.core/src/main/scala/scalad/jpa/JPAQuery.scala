package scalad.jpa

import scalad.{GroupBy, OrderBy, Restriction, Query}


/**
 * @author janmachacek
 */

class JPAQuery(restriction: Restriction,
             orderByClauses: List[OrderBy],
             groupByClauses: List[GroupBy])
  extends Query(restriction, orderByClauses, groupByClauses) {

  def inner(join: Join) = this
  
  def outer(join: Join) = this

}

final class Join(path: String, inner: Boolean, eager: Boolean) {
  
  def fetch = new Join(path, inner, true)
  
}
