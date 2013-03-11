package org.cakesolutions.scalad

import language.postfixOps

trait JsonDsl {

  import spray.json._

  implicit def jsonObjectWriter = new JsonWriter[JsObject] {
    def write(value: JsObject) = value
  }

  implicit def jsonArrayWriter = new JsonWriter[JsArray] {
    def write(value: JsArray) = value
  }

  def $[V: JsonFormat](elements: V*): JsArray = {
    val cf = new CollectionFormats {}
    cf.listFormat.write(elements.toList)
  }

  implicit class JsObjectBuilder[V: JsonWriter](key: String) extends DefaultJsonProtocol {
    val writer = implicitly[JsonWriter[V]]
    def :>(that: V): JsObject = JsObject(Map(key -> writer.write(that)))
  }

  implicit class JsObjectMonoidalMappend(obj: JsObject) extends DefaultJsonProtocol {
    def <>(that: JsObject) = (obj.fields ++ that.fields).toJson
    def ~(that: JsObject) = obj <> that
  }
}
