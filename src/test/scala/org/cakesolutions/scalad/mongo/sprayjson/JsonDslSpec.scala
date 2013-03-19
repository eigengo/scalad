package org.cakesolutions.scalad.mongo.sprayjson

import spray.json._
import org.specs2.mutable.Specification

class JsonDslSpec extends Specification with DefaultJsonProtocol {

  import org.cakesolutions.scalad.mongo.sprayjson._

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
      val res = "foo" :> {"bar" :> 10}
      val expected = Map("foo" -> Map("bar" -> 10)).toJson
      res.compactPrint mustEqual(expected.compactPrint)
    }

    "allow monoid-like mappending of objects" in {
      val a1 = {"foo" :> {"bar" :> 10}} <> {"age" :> 45}
      val expected = a1.compactPrint
      expected mustEqual("""{"foo":{"bar":10},"age":45}""")
    }

    "correctly handle JSON arrays" in {
      val a1 = $(1,2,3)
      val expected = a1.compactPrint
      expected mustEqual("[1,2,3]")
    }

    "Correctly handle combination of nested object and arrays" in {
      val a1 = {"lorem" :> $("ipsum", "lorem ipsum")}
      val a2 = {"foo" :> 10} ~ {"bar" :> a1}
      val expected = a2.compactPrint
      expected mustEqual("""{"foo":10,"bar":{"lorem":["ipsum","lorem ipsum"]}}""")
    }

  }
}
