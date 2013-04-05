package org.eigengo.scalad.experimental

import org.specs2.mutable.Specification
import org.eigengo.scalad.mongo.LongEntity


class RestrictionsSpec extends Specification with Restrictions with StringRestrictionsPaths with SpecNativeRestrictions {

  noopNativeRestrictionMarshaller[Any]

  "trivial query" in {
    val le = LongEntity(1, "foo")
    val query: SpecNativeRestriction = "someobj" equalTo le
    query.r mustEqual EqualsRestriction("someobj", le)
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

}

case class SpecNativeRestriction(r: Restriction)

trait SpecNativeRestrictions extends NativeRestrictions {

  type NativeRestriction = SpecNativeRestriction

  def convertToNative(restriction: Restriction) = SpecNativeRestriction(restriction)

  implicit def noopNativeRestrictionMarshaller[A] = new NativeRestrictionsMarshaller[A] {
    type NativeRestrictionValue = A
    def marshal(value: A) = value
  }
}

