package org.cakesolutions.scalad

import spray.json._
import org.specs2.mutable.Specification

class JsonDslSpec extends Specification with JsonDsl with DefaultJsonProtocol {

  sequential

  "the JSON DSL should" should {

    "produce a valid JsValue out of a simple key/String mapping" in {
      val res = {"foo" :> "bar"}
      val expected = Map("foo" -> "bar").toJson
      res.compactPrint mustEqual(expected.compactPrint)
    }

    "produce a valid JsValue out of a simple key/Int mapping" in {
      val res = {"foo" :> 10}
      val expected = Map("foo" -> 10).toJson
      res.compactPrint mustEqual(expected.compactPrint)
    }

    "produce a valid JsValue out of a simple key/Boolean mapping" in {
      val res = {"foo" :> true}
      val expected = Map("foo" -> true).toJson
      res.compactPrint mustEqual(expected.compactPrint)
    }

    "produce a valid JsValue out of a nested mapping" in {
      val res = {"foo" :> {"bar" :> 10}}
      val expected = Map("foo" -> Map("bar" -> 10)).toJson
      res.compactPrint mustEqual(expected.compactPrint)
    }

  }
}
