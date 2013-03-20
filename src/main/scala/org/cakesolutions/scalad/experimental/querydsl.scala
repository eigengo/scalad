package org.cakesolutions.scalad.experimental

import scala.language.implicitConversions

sealed trait Restriction
case class EqualsRestriction[A, Path](path: Path, value: A) extends Restriction with RestrictionOps
case class NotEqualsRestriction[A, Path](path: Path, value: A) extends Restriction with RestrictionOps
case class OrdRestriction[A : Ordering, Path](path: Path, ord: Symbol, value: A) extends Restriction with RestrictionOps

case class NotRestriction(restriction: Restriction) extends Restriction

case class ConjunctionRestriction(lhs: Restriction, rhs: Restriction) extends Restriction with RestrictionOps
case class DisjunctionRestriction(lhs: Restriction, rhs: Restriction) extends Restriction with RestrictionOps

case object ContradictionRestriction extends Restriction
case object TautologyRestriction extends Restriction

/**
 * Contains functions that construct trees of expressions
 */
trait RestrictionOps {
  this: Restriction =>

  /**
   * Combines this restriction with that restriction in a conjunction
   *
   * @param that the right hand side
   * @return this && that
   */
  def &&(that: Restriction) = ConjunctionRestriction(this, that)

  /**
   * Combines this restriction with that restriction in a disjunction
   *
   * @param that the right hand side
   * @return this || that
   */
  def ||(that: Restriction) = DisjunctionRestriction(this, that)

}

/**
 * Contains functions to simplify restrictions
 */
trait RestrictionSimplification {

  private def simplifyConjunction(conjunction: ConjunctionRestriction): Restriction = conjunction match {
    case ConjunctionRestriction(lhs, rhs) if lhs == rhs    => lhs
    case ConjunctionRestriction(_, ContradictionRestriction) => ContradictionRestriction
    case ConjunctionRestriction(ContradictionRestriction, _) => ContradictionRestriction
    case ConjunctionRestriction(EqualsRestriction(p1, v1), NotEqualsRestriction(p2, v2)) if p1 == p2 && v1 == v2 => ContradictionRestriction
    case ConjunctionRestriction(NotEqualsRestriction(p1, v1), EqualsRestriction(p2, v2)) if p1 == p2 && v1 == v2 => ContradictionRestriction
    case ConjunctionRestriction(lhs, rhs) =>
      val simplerLhs = simplify(lhs)
      val simplerRhs = simplify(rhs)
      if (simplerLhs != lhs || simplerRhs != rhs) simplify(ConjunctionRestriction(simplerLhs, simplerRhs)) else conjunction
  }

  private def simplifyDisjunction(disjunction: DisjunctionRestriction): Restriction = disjunction match {
    case DisjunctionRestriction(lhs, rhs) if lhs == rhs => lhs
    case DisjunctionRestriction(_, TautologyRestriction) => TautologyRestriction
    case DisjunctionRestriction(TautologyRestriction, _) => TautologyRestriction
    case DisjunctionRestriction(EqualsRestriction(p1, v1), NotEqualsRestriction(p2, v2)) if p1 == p2 && v1 == v2 => TautologyRestriction
    case DisjunctionRestriction(NotEqualsRestriction(p1, v1), EqualsRestriction(p2, v2)) if p1 == p2 && v1 == v2 => TautologyRestriction
    case DisjunctionRestriction(lhs, rhs) =>
      val simplerLhs = simplify(lhs)
      val simplerRhs = simplify(rhs)
      if (simplerLhs != lhs || simplerRhs != rhs) simplify(DisjunctionRestriction(simplerLhs, simplerRhs)) else disjunction
  }

  final def simplify(restriction: Restriction): Restriction = restriction match {
    case c: ConjunctionRestriction => simplifyConjunction(c)
    case d: DisjunctionRestriction => simplifyDisjunction(d)
    case _                         => restriction
  }

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

trait NativeRestrictions extends RestrictionSimplification {
  type NativeRestriction

  implicit final def doConvertToNative(restriction: Restriction) = convertToNative(simplify(restriction))

  def convertToNative(restriction: Restriction): NativeRestriction

}

/**
 * Provides the starting point for the restrictions DSL
 */
trait Restrictions {
  type RestrictionPath

  import language.implicitConversions

  /**
   * Begins constructing the restriction by turning the ``RestrictionPath`` into an instance of the
   * ``RestrictionBuilder``. You can then call its ``equalTo``, ``lessThan`` and other methods.
   *
   * @param path the starting path
   * @return the ``RestrictionBuilder`` starting from the path
   */
  implicit final def beginRestriction(path: RestrictionPath): RestrictionBuilder[RestrictionPath] = new RestrictionBuilder(path)

}

trait NativeRestrictionsMarshaller[A] {

  type NativeRestrictionValue

  def marshal(value: A): NativeRestrictionValue

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
  def equalTo[A](value: A)(implicit marshaller: NativeRestrictionsMarshaller[A]) = EqualsRestriction(path, marshaller.marshal(value))

  /**
   * Property is not equal to the given value
   *
   * @param value the value the ``path`` must not be equal to
   * @tparam A the type of the value
   * @return the != restriction
   */
  def notEqualTo[A](value: A)(implicit marshaller: NativeRestrictionsMarshaller[A]) = NotEqualsRestriction(path, marshaller.marshal(value))

  // def lessThan[A : Ordering](value: A) = OrdRestriction(path, '<, nativeValue(value))

}