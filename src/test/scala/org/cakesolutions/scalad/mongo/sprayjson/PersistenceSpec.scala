package org.cakesolutions.scalad.mongo.sprayjson

import java.util.UUID
import org.specs2.mutable.Specification

class PersistenceSpec extends Specification with SprayJsonTestSupport {

  sequential

  val crud = new SprayMongo

  val student = Student(
    101287,
    UUID.randomUUID(),
    "Alfredo",
    List(Person("John", "Doe"), Person("Mary", "Lamb")),
    Address("Foo Rd.", 91),
    graduated = true
  )

  implicit val StudentCollectionProvider = new SprayMongoCollection[Student](db, "students", List("id":>1))

  "SprayJsonSerialisation" should {

    "ensure a Student is created" in {
      crud.create(student) === Some(student)
    }

    "ensure a Student is searchable by id" in {
      crud.searchFirst[Student]("id":>student.id) === Some(student)
    }

    "ensure a Student is searchable by name" in {
      crud.searchFirst[Student]("name":>student.name) === Some(student)
    }

    "ensure a Student is searchable by UUID" in {
      crud.searchFirst[Student]("collegeUuid":>student.collegeUuid) === Some(student)
    }

    "ensure a Student is searchable by nested JSON query" in {
      crud.searchFirst[Student]("address":> student.address) === Some(student)
      crud.searchFirst[Student]("address":> {"road":> student.address.road <> "number":> student.address.number}) === Some(student)
    }
  }
}
