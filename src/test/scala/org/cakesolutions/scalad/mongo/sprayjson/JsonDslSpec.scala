package org.cakesolutions.scalad.mongo.sprayjson

import spray.json._
import org.specs2.mutable.Specification

class JsonDslSpec extends Specification with DefaultJsonProtocol {

  import org.cakesolutions.scalad.mongo.sprayjson._

  sequential

  "the JSON DSL should" should {

    "produce a valid JsValue out of a simple key/String mapping" in {
      {"foo" :> "bar"} === JsonParser("""{"foo":"bar"}""")
    }

    "produce a valid JsValue out of a simple key/Int mapping" in {
      {"foo" :> 10} === JsonParser("""{"foo":10}""")
    }

    "produce a valid JsValue out of a simple key/Boolean mapping" in {
      {"foo" :> true} === JsonParser("""{"foo":true}""")
    }

    "produce a valid JsValue out of a nested mapping" in {
      "foo" :> {"bar" :> 10} === JsonParser("""{"foo": {"bar":10}}""")
    }

    "allow monoid-like mappending of objects" in {
     ({"foo" :> {"bar" :> 10}} <> {"age" :> 45}) === JsonParser("""{"foo":{"bar":10},"age":45}""")
    }

    "correctly handle JSON arrays" in {
      $(1,2,3) === JsonParser("[1,2,3]")
    }

    "Correctly handle combination of nested object and arrays" in {
      val a1 = {"lorem" :> $("ipsum", "lorem ipsum")}
      {"foo" :> 10} ~ {"bar" :> a1} === JsonParser("""{"foo":10,"bar":{"lorem":["ipsum","lorem ipsum"]}}""")
    }

  }
}
