package scalad

/**
 * @author markh
 */

trait OrmLike {

  implicit def toOrmQuery(q: Query) = new OrmQuery(q.restriction, q.orderByClauses, q.groupByClauses, None, Nil)

  implicit def toOrmQuery(r: Restriction) = new OrmQuery(r, Nil, Nil, None, Nil)

  implicit def toPath(expression: String) = new PartialPath(expression)

}