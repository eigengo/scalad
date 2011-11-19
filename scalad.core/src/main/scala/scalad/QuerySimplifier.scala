package scalad

/**
 * The simplifier attempts to simplify the queries by identifying tautologies and contradictions,
 * thus making it easier for the underlying data store to execute the query.<br/>
 *
 * <note>
 * Ideally, all queries would reduce down to {{Contradiction()}}, which would mean that
 * there's nothing to be done.
 * </note>
 *
 * @author janmachacek
 */
class QuerySimplifier {

  def simplify(query: Query) = {
    val newRestriction = query.restriction match {
      case d: Disjunction => simplifyDisjunction(d)
      case c: Conjunction => simplifyConjunction(c)
      case x => x
    }
    
    new Query(newRestriction, query.orderByClauses, query.groupByClauses)
  }

  def simplifyDisjunction(d: Disjunction): Restriction = {
    (d.lhs.restriction, d.rhs.restriction) match {
      case (Tautology(), Tautology()) => Tautology()
      case (Contradiction(), Contradiction()) => Contradiction()
      case (Tautology(), rhs) if rhs != Tautology() => rhs
      case (lhs, Tautology()) if lhs != Tautology() => lhs
      case (Binary(p1, '==, v1), Binary(p2, '!=, v2)) if (p1 == p2 && v1 == v2) => Tautology()
      case (Binary(p1, '!=, v1), Binary(p2, '==, v2)) if (p1 == p2 && v1 == v2) => Tautology()
      case (d1: Disjunction, d2: Disjunction) =>
        (simplifyDisjunction(d1), simplifyDisjunction(d2)) match {
          case (Tautology(), _) => Tautology()
          case (_, Tautology()) => Tautology()
          case _ => d
        }
      case (lhs, d2: Disjunction) =>
        simplifyDisjunction(d2) match {
          case Tautology() => lhs
          case x => x
        }
      case (d2: Disjunction, rhs) =>
        simplifyDisjunction(d2) match {
          case Tautology() => rhs
          case x => x
        }
      case _ => d
    }
  }
  
  def simplifyConjunction(c: Conjunction): Restriction = {
    (c.lhs.restriction, c.rhs.restriction) match {
      case (Tautology(), Tautology()) => Tautology()
      case (Tautology(), _) => Tautology()
      case (_, Tautology()) => Tautology()
      case (Contradiction(), _) => Contradiction()
      case (_, Contradiction()) => Contradiction()
      case (Binary(p1, '==, v1), Binary(p2, '!=, v2)) if (p1 == p2 && v1 == v2) => Contradiction()
      case (Binary(p1, '!=, v1), Binary(p2, '==, v2)) if (p1 == p2 && v1 == v2) => Contradiction()
      case (Binary(p1, _, v1), IsNull(p2)) if (p1 == p2) => Contradiction()
      case (c1: Conjunction, c2: Conjunction) =>
        (simplifyConjunction(c1), simplifyConjunction(c2)) match {
          case (Contradiction(), _) => Contradiction()
          case (_, Contradiction()) => Contradiction()
          case _ => c
        }
      case (lhs, c2: Conjunction) =>
        simplifyConjunction(c2) match {
          case Contradiction() => Contradiction()
          case _ => c
        }
      case (c2: Conjunction, rhs) =>
        simplifyConjunction(c2) match {
          case Contradiction() => Contradiction()
          case _ => c
        }
      case _ => c
    }
  }
  
}