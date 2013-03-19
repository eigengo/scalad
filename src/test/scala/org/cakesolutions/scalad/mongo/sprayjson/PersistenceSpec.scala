package org.cakesolutions.scalad.mongo.sprayjson

import java.util.UUID
import org.specs2.mutable.Specification

class PersistenceSpec extends Specification with SprayJsonTestSupport {

  "SprayJsonSerialisation" should {
    sequential

    val crud = ??? //new MongoCrud

    val studUuid = UUID.randomUUID()
    val student = Student(
      101287,
      studUuid,
      "Alfredo",
      List(Person("John", "Doe"), Person("Mary", "Lamb")),
      Address("Foo Rd.", 91),
      graduated = true
    )

    "ensure a Student is correctly persisted" in {
      //crud.create(student).get must beEqualTo(student)
      todo
    }

    "ensure a Student is searchable by id" in {
      todo
    }

    "ensure a Student is searchable by name" in {
      todo
    }

    "ensure a Student is searchable by UUID" in {
      todo
    }

    "ensure a Student is searchable by nested JSON query" in {
      //  """{"address": {"road": "Foo Rd.", "number": 91}}"""
      todo
    }
  }
}
