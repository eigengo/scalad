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
  
  def innerJoin(path: Path) = join(Join(path, true, false))
  
  def outerJoin(path: Path) = join(Join(path, false, false))
  
  def innerJoinFetch(path: Path) = join(Join(path, true, true))
  
  def outerJoinFetch(path: Path) = join(Join(path, false, true))

  def innerJoin(path: String) = join(Join(Path(path, path), true, false))

  def outerJoin(path: String) = join(Join(Path(path, path), false, false))

  def innerJoinFetch(path: String) = join(Join(Path(path, path), true, true))

  def outerJoinFetch(path: String) = join(Join(Path(path, path), false, true))

  override def simplify = new JPAQuery(simplifyRestriction(restriction), orderByClauses, groupByClauses, pageOption, joins)

  override def page(range: Range) = new JPAQuery(restriction, orderByClauses, groupByClauses, Some(Page(range.start, range.end)), joins)
}

/*

final class Path(private val expression: String) {
  val path = if (expression.indexOf(' ') == -1) expression else expression.split(' ')(0)
  val alias = if (expression.indexOf(' ') == -1) expression else expression.split(' ')(1)  
}
*/

class PartialPath(val expression: String) {
  
  def as(alias: String) = Path(expression, alias)
  
}

final case class Path(expression: String, alias: String)

final case class Join(path: Path, inner: Boolean, eager: Boolean)