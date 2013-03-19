package org.cakesolutions.scalad.experimental

import org.cakesolutions.scalad._
import com.mongodb.{BasicDBObjectBuilder, DBObject}
import mongo.MongoSerialiser
import mongo.sprayjson.SprayJsonSerialisers

/**
 * Native restrictions for JsObject and MongoDB
 */
trait MongoJsonNativeRestrictions extends NativeRestrictions with MongoNativeRestrictionMarshallers {
  type NativeRestriction = DBObject

  def convertToNative(restriction: Restriction) = {
    def convert0(builder: BasicDBObjectBuilder, r: Restriction) {
      r match {
        case EqualsRestriction(path: String, value) => builder.add(path, value)
        case NotEqualsRestriction(path: String, value) => builder.add(path, value)
        case ConjunctionRestriction(lhs, rhs) => convert0(builder, lhs); convert0(builder, rhs)
        case DisjunctionRestriction(lhs, rhs) => convert0(builder, lhs); convert0(builder, rhs)
        // ...
      }
    }

    val builder = BasicDBObjectBuilder.start()
    convert0(builder, restriction)
    builder.get()
  }

}

private[experimental] trait MongoNativeRestrictionMarshallers extends SprayJsonSerialisers {

  implicit def getNativeRestrictionsMarshaller[A: MongoSerialiser]: NativeRestrictionsMarshaller[A] = new NativeRestrictionsMarshaller[A] {
    val serialiser = implicitly[MongoSerialiser[A]]

    type NativeRestrictionValue = DBObject

    def marshal(value: A) = serialiser.serialiseDB(value)
  }

}
