package org.eigengo.scalad.mongo.sprayjson

import java.util.UUID
import org.eigengo.scalad.mongo.MongoCrudTestAccess
import spray.json.{JsValue, JsonFormat, DefaultJsonProtocol}
import akka.contrib.jul.JavaLogging
import com.mongodb.DBObject
import org.specs2.mutable.Specification

//Bogus domain-like entities
case class Person(name: String, surname: String, id: UUID = UUID.randomUUID())
case class Address(road: String, number: Int)
case class Student(id: Long,
                   collegeUuid: UUID,
                   name: String,
                   parents: List[Person],
                   address: Address,
                   graduated: Boolean)


trait SprayJsonTestSupport extends MongoCrudTestAccess
with DefaultJsonProtocol with UuidMarshalling with DateMarshalling with BigNumberMarshalling with JavaLogging {
  this: Specification =>

  implicit val PersonFormatter = jsonFormat3(Person)
  implicit val AddressFormatter = jsonFormat2(Address)
  implicit val StudentFormatter = jsonFormat6(Student)

}