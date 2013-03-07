package org.cakesolutions.scalad

import language.postfixOps

trait JsonDsl {

  import spray.json._

  implicit class JsObjectBuilder(key: String) extends DefaultJsonProtocol {

    def :>[A : JsonFormat](that: A) = Map(key -> that).toJson
  }
}
