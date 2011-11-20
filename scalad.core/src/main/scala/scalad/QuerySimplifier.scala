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
    val newRestriction = simplifyRestriction(query.restriction)
    new Query(newRestriction, query.orderByClauses, query.groupByClauses)
  }
  
  private def simplifyRestriction(r: Restriction) = r match {
    case d: Disjunction => simplifyDisjunction(d)
    case c: Conjunction => simplifyConjunction(c)
    case In(property, values) if (values.isEmpty) => Contradiction()
    case x => x
  }

  private def simplifyDisjunction(d: Disjunction): Restriction = {
    (d.lhs, d.rhs) match {
      // trivial simplifications
      case (Tautology(), Tautology()) => Tautology()
      case (Contradiction(), Contradiction()) => Contradiction()
      case (Tautology(), rhs) if rhs != Tautology() => rhs
      case (lhs, Tautology()) if lhs != Tautology() => lhs

      // equals & duplicates
      case (b@Binary(p1, op1, v1), Binary(p2, op2, v2)) if (p1 == p2 && v1 == v2 && op1 == op2) => b
      case (Binary(p1, '==, v1), Binary(p2, '!=, v2)) if (p1 == p2 && v1 == v2) => Tautology()
      case (Binary(p1, '!=, v1), Binary(p2, '==, v2)) if (p1 == p2 && v1 == v2) => Tautology()

      case (lhs, rhs) =>
        simplifyDisjunction(Disjunction(simplifyRestriction(lhs), simplifyRestriction(rhs)))
    }
  }
  
  private def simplifyConjunction(c: Conjunction): Restriction = {
    (c.lhs, c.rhs) match {
      // trivial simplifications
      case (Tautology(), Tautology()) => Tautology()
      case (Contradiction(), _) => Contradiction()
      case (_, Contradiction()) => Contradiction()
      case (Tautology(), rhs) if (rhs != Conjunction) => simplifyRestriction(rhs)
      case (lhs, Tautology()) if (lhs != Conjunction)=> simplifyRestriction(lhs)

      // equals & duplicates
      case (b@Binary(p1, op1, v1), Binary(p2, op2, v2)) if (p1 == p2 && v1 == v2 && op1 == op2) => b
      case (Binary(p1, '==, v1), Binary(p2, '!=, v2)) if (p1 == p2 && v1 == v2) => Contradiction()
      case (Binary(p1, '!=, v1), Binary(p2, '==, v2)) if (p1 == p2 && v1 == v2) => Contradiction()
      case (Binary(p1, '==, v1), Binary(p2, '==, v2)) if (p1 == p2 && v1 != v2) => Contradiction()
      case (Binary(p1, _, v1), IsNull(p2)) if (p1 == p2) => Contradiction()

      // it is a conjunction of some other restrictions
      case (lhs, rhs) =>
        simplifyConjunction(Conjunction(simplifyRestriction(lhs), simplifyRestriction(rhs)))
    }
  }
  
}