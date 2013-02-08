package org.cakesolutions.scalad

import mongo._
import mongo.LongEntity
import org.specs2.mutable.Specification
import com.mongodb.DBObject

class RestrictionsSpec extends Specification with Restrictions with StringRestrictionsPaths with SprayJsonNativeRestrictions
  with LongEntityPersistence {

  "trivial query" in {
    val le = LongEntity(1, "foo")
    val query: DBObject = "someobj" equalTo le
    println(query)
    success
    //query.r mustEqual EqualsRestriction("username", "foo")
  }

//  "conjunctions and disjunctions" should {
//
//    "combine simple restrictions" in {
//      val query: SpecNativeRestriction = ("username" equalTo "foo") && ("password" equalTo "merde embulante")
//      query.r mustEqual ConjunctionRestriction(EqualsRestriction("username", "foo"),
//                                             EqualsRestriction("password", "merde embulante"))
//    }
//
//    "simplify well" in {
//      val query: SpecNativeRestriction = ("username" equalTo "foo") && ("username" notEqualTo "foo")
//      query.r mustEqual ContradictionRestriction
//    }
//
//  }

}