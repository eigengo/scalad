package org.cakesolutions.scalad.mongo

import spray.json.{JsString, JsObject}
import org.cakesolutions.scalad.{EqualsRestriction, NativeRestrictions, Restriction}

/**
 * Native restrictions for JsObject and MongoDB
 */
trait SprayJsonNativeRestrictions extends NativeRestrictions {
  type NativeRestriction = JsObject

  def toNative(restriction: Restriction) = JsObject(("x", JsString("x")))
}
