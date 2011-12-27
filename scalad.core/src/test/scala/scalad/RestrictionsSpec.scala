package scalad

import org.specs2.mutable.Specification
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

/**
 * @author janmachacek
 */
@RunWith(classOf[JUnitRunner])
class RestrictionsSpec extends Specification with OrmLike {
  import Scalad._

  "none in options" in {
    "must reduce to nothing when None"          ! restrictionMatch((id ＝ None) || ("field" ＝ None), Skip())
  }

  "guard" in {
    "must reduce to nothing when `false`"       ! restrictionMatch((id ＝ "a" when false), Skip())

    "must reduce to LHS in || when `false`"     ! restrictionMatch(((id ＝ 5) || ("foo" ＝ "a" when false)), (id ＝ 5))
    "must reduce to RHS in || when `false`"     ! restrictionMatch((("foo" ＝ "a" when false) || (id ＝ 5)), (id ＝ 5))

    "must reduce to LHS in && when `false`"     ! restrictionMatch(((id ＝ 5) && ("foo" ＝ "a" when false)), (id ＝ 5))
    "must reduce to RHS in && when `false`"     ! restrictionMatch((("foo" ＝ "a" when false) && (id ＝ 5)), (id ＝ 5))
  }

  "tautology in disjunction" in {
    "tautology `foo=a || foo!=a`"               ! restrictionMatch(((id ＝ "a") || (id !＝ "a")), Tautology())
    "tautology `foo=a || foo!=a`"               ! restrictionMatch(((id ＝ "a") || (id !＝ "a")), Tautology())
  }

  "reduction in disjunction" in {
    "must reduce to LHS"                        ! restrictionMatch(((id ＝ "a") || ((id !＝ "a") && (id ＝ "a"))), (id ＝ "a"))
    "must reduce to RHS"                        ! restrictionMatch((((id ＝ "a") || (id ＝ "a")) || (id ＝ "a")), (id ＝ "a"))
  }

  "contradiction in conjunction" in {
    "must contradict `foo=a && foo!=a`"         ! restrictionMatch((("foo" ＝ "a") && ("foo" !＝ "a")), Contradiction())
    "must contradict `foo=a && foo=b`"          ! restrictionMatch((("foo" ＝ "a") && ("foo" ＝ "B")), Contradiction())

    "contradict `id=a && (id=a && id!=a)`"      ! restrictionMatch(((id ＝ "a") && ((id !＝ "a") && (id ＝ "a"))), Contradiction())
    "contradict `(id=a && id!=a) && id=a`"      ! restrictionMatch((((id ＝ "a") && (id !＝ "a")) && (id ＝ "a")), Contradiction())

    "contradict `id=a && id is null`"           ! restrictionMatch((id ＝ "a") && (id isNull), Contradiction())
  }

  "duplicate terms" in {
    "remove dups in foo=a && (foo=a && foo=a)"  ! restrictionMatch(("foo" ＝ "a") && (("foo" ＝ "a") && ("foo" ＝ "a")), ("foo" ＝ "a"))
    "remove dups in foo=a || (foo=a || foo=a)"  ! restrictionMatch(("foo" ＝ "a") || (("foo" ＝ "a") && ("foo" ＝ "a")), ("foo" ＝ "a"))
    "remove dups in foo=a || foo in(a, b, c)"   ! restrictionMatch(("foo" ＝ "a") || ("foo" isIn("a", "b", "c")), ("foo" isIn("a", "b", "c")))
  }

  private def restrictionMatch(rr: Restriction, r: Restriction)(implicit simplifier: RestrictionSimplifier) =
    simplifier.simplifyRestriction(rr) must_== r

}