package org.cakesolutions.scalad

import org.specs2.mutable.Specification

class RestrictionsSpec extends Specification with Restrictions with StringRestrictionsPaths with NativeRestrictions {

  "foo" in {
    val query = "username" equalTo "foo"
    query mustEqual EqualsRestriction("username", "foo")
  }

  type NativeRestriction = Restriction

  def convertNative(restriction: Restriction) = restriction
}