package scalad

/**
 * Trait to be mixed-in to ORM-like implementations
 *
 * @author markh
 */
trait OrmLike {

  implicit def toOrmQuery(q: OldQuery) = new OrmQuery(q.restriction, q.orderByClauses, q.groupByClauses, None, Nil)

  implicit def toOrmQuery(r: Restriction) = new OrmQuery(r, Nil, Nil, None, Nil)

  implicit def toPath(expression: String) = new PartialPath(expression)

  implicit def toPartialRestriction(property: String) = new PartialRestriction(NamedProperty(property))

  implicit def toPartialRestriction(id: Identity) = new PartialRestriction(id)

  implicit def toPartialOrder(property: String) = new PartialOrder(NamedProperty(property))

  implicit def toGroupBy(property: String) = GroupBy(NamedProperty(property))

}