package org.cakesolutions.scalad.mongo

import spray.json.{JsString, JsObject}
import org.cakesolutions.scalad.{NativeRestrictions, Restriction}

/**
 * Native restrictions for JsObject and MongoDB
 */
trait SprayJsonNativeRestrictions extends NativeRestrictions {
  type NativeRestriction = JsObject

  def toNative(restriction: Restriction) = JsObject(("foo", JsString("bar")))
}
