package org.cakesolutions.scalad

import language.postfixOps

trait JsonDsl {

  import spray.json._

  implicit def jsonObjectWriter = new JsonWriter[JsObject] {
    def write(value: JsObject) = value
  }

  implicit class JsObjectBuilder[V: JsonWriter](key: String) extends DefaultJsonProtocol {
    val writer = implicitly[JsonWriter[V]]
    def :>(that: V): JsObject = JsObject(Map(key -> writer.write(that)))
  }

  implicit class JsObjectMonoidalMappend(obj: JsObject) extends DefaultJsonProtocol {
    def <>(that: JsObject) = (obj.fields ++ that.fields).toJson
  }
}
