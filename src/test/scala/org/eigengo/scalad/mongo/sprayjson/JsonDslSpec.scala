package org.eigengo.scalad.mongo.sprayjson

import spray.json._
import org.specs2.mutable.Specification
import org.bson.types.ObjectId
import org.joda.time.{DateTimeZone, DateTime}

class JsonDslSpec extends Specification with DefaultJsonProtocol with NullMarshalling with DateMarshalling with ObjectIdMarshalling {
  import org.eigengo.scalad.mongo.sprayjson._

  sequential

  "the JSON DSL should" should {

    "produce a valid JsValue out of a simple key/String mapping" in {
      {
        "foo" :> "bar"
      } === JsonParser( """{"foo":"bar"}""")
    }

    "produce a valid JsValue out of a simple key/Int mapping" in {
      {
        "foo" :> 10
      } === JsonParser( """{"foo":10}""")
    }

    "produce a valid JsValue out of a simple key/Boolean mapping" in {
      {
        "foo" :> true
      } === JsonParser( """{"foo":true}""")
    }

    "produce a valid JsValue out of a nested mapping" in {
      "foo" :> {
        "bar" :> 10
      } === JsonParser( """{"foo": {"bar":10}}""")
    }

    "allow monoid-like mappending of objects" in {
      ("foo" :> { "bar" :> 10 } <>
       "age" :> 45 <>
       "base" :> 50
      ) === JsonParser( """{"foo":{"bar":10},"age":45,"base":50}""")
    }

    "correctly handle JSON arrays" in {
      $(1, 2, 3) === JsonParser("[1,2,3]")
    }

    "Correctly round-trips ObjectIds" in {
      val oId = new ObjectId("53627de05ed0089ad3c1cdf9")
      "id" :> oId === JsonParser("""{"id": { "$oid": "53627de05ed0089ad3c1cdf9" } }""")
      JsonParser(oId.toJson.toString).convertTo[ObjectId] === oId
    }

    "Correctly round-trips Dates" in {
      val d = new DateTime("2001-1-1", DateTimeZone.UTC)
      "date" :> d === JsonParser("""{"date": { "$date": "2001-01-01T00:00:00.000Z" } }""")
      JsonParser(d.toJson.toString).convertTo[DateTime].withZone(DateTimeZone.UTC) === d
    }

    "Correctly handle combination of nested object and arrays" in {
      val a1 = {
        "lorem" :> $("ipsum", "lorem ipsum")
      }
      {
        "foo" :> 10
      } ~ {
        "bar" :> a1
      } === JsonParser( """{"foo":10,"bar":{"lorem":["ipsum","lorem ipsum"]}}""")
    }

    "correctly handle null" in {
      "foo" :> null === JsonParser( """{"foo":null}""")
    }

    "symbols are allowed for field names" in {
      'foo :> 5 === JsonParser("""{"foo": 5}""")
    }

  }
}
