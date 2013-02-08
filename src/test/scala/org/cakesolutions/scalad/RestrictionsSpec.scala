package org.cakesolutions.scalad

import org.specs2.mutable.Specification

class RestrictionsSpec extends Specification with Restrictions with StringRestrictionsPaths with NativeRestrictions {

  "trivial query" in {
    val query: SpecNativeRestriction = "username" equalTo "foo"
    query.r mustEqual EqualsRestriction("username", "foo")
  }

  "conjunctions and disjunctions" should {

    "combine simple restrictions" in {
      val query: SpecNativeRestriction = ("username" equalTo "foo") && ("password" equalTo "merde embulante")
      query.r mustEqual ConjunctionRestriction(EqualsRestriction("username", "foo"),
                                             EqualsRestriction("password", "merde embulante"))
    }

    "simplify well" in {
      val query: SpecNativeRestriction = ("username" equalTo "foo") && ("username" notEqualTo "foo")
      query.r mustEqual ContradictionRestriction
    }

  }

  case class SpecNativeRestriction(r: Restriction)

  type NativeRestriction = SpecNativeRestriction

  def convertNative(restriction: Restriction) = SpecNativeRestriction(restriction)
}