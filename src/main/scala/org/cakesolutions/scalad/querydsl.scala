package org.cakesolutions.scalad

sealed trait Restriction
case class EqualsRestriction[A, Path](path: Path, value: A) extends Restriction
case class OrdRestriction[A : Ordering, Path](path: Path, ord: Symbol, value: A) extends Restriction

case class NotRestriction(restriction: Restriction) extends Restriction

case class ConjunctionRestriction(lhs: Restriction, rhs: Restriction) extends Restriction
case class DisjunctionRestriction(lhs: Restriction, rhs: Restriction) extends Restriction

/**
 * Translates some ``Restriction`` into its native representation. This will differ database-by-database
 */
trait NativeRestrictions {
  type NativeRestriction

  def convertNative(restriction: Restriction): NativeRestriction

}

/**
 * Defines the ``RestrictionPath`` to be string, which most databases are happy with:
 * - in SQL databases, the ``RestrictionPath`` is the column name,
 * - in Mongo, the ``RestrictionPath`` is the property name,
 * - ...
 */
trait StringRestrictionsPaths {
  type RestrictionPath = String
}

/**
 * Provides the starting point for the restrictions DSL
 */
trait Restrictions {
  native: NativeRestrictions =>
  type RestrictionPath
  type NativeRestriction

  import language.implicitConversions

  implicit def beginRestriction(path: RestrictionPath): RestrictionBuilder[RestrictionPath] = new RestrictionBuilder(path)

  implicit def endRestriction(restriction: Restriction): NativeRestriction = native.convertNative(restriction)

}

/**
 * Begins the construction of the restrictions so that you can construct the entire query tree
 *
 * @param path the starting path, i.e. "username" ... so that you can construct things like "username" == "foo"
 * @tparam Path the type of the path
 */
class RestrictionBuilder[Path](path: Path) {

  /**
   * Property is equal to the given value
   *
   * @param value the value the ``path`` must be equal to
   * @tparam A the type of the value
   * @return the == restriction
   */
  def equalTo[A](value: A) = EqualsRestriction(path, value)

  def lessThan[A : Ordering](value: A) = OrdRestriction(path, '<, value)

  def greaterThan[A : Ordering](value: A) = OrdRestriction(path, '>, value)

}