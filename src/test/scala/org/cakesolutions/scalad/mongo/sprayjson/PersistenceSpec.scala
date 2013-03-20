package org.cakesolutions.scalad.mongo.sprayjson

import java.util.UUID
import org.specs2.mutable.Specification
import spray.json.JsNull
import com.mongodb.MongoException

class PersistenceSpec extends Specification with SprayJsonTestSupport with NullMarshalling {

  sequential

  val crud = new SprayMongo

  val student = Student(
    101287,
    UUID.randomUUID(),
    "Alfredo",
    List(Person("John", "Doe"), Person("Mary", "Lamb")),
    Address("Foo Rd.", 91),
    graduated = false
  )

  val modified = student.copy(graduated = true)

  implicit val StudentCollectionProvider = new SprayMongoCollection[Student](db, "students", List("collegeUuid":>1), List("id":>1))

  "SprayJsonSerialisation" should {

    "ensure a Student is created" in {
      crud.create(student) === Some(student)
    }

    "ensure the uniqueness constraint is respected" in {
      crud.create(student) should throwA[MongoException]
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

    "ensure a Student is modifyable" in {
      crud.findAndModify[Student]("id":>student.id, "$set":>{"graduated":> modified.graduated})
      crud.searchFirst[Student]("id":>student.id) === Some(modified)
    }

    "ensure a Student is replaceable" in {
      crud.findAndReplace[Student]("id":>student.id, student)
      crud.searchFirst[Student]("id":>student.id) === Some(student)
    }

    "ensure a Student can be deleted" in {
      crud.deleteFirst[Student]("id":>student.id)
      crud.searchFirst[Student]("id":>student.id) === None
    }

    "ensure we can run aggregate queries on Students" in {
      crud.create(student)
      crud.create(student.copy(id = 1))
      crud.create(student.copy(id = 2))
      crud.create(student.copy(id = 3, name = "Evil Alfredo"))
      crud.create(student.copy(id = 4, collegeUuid = UUID.randomUUID()))
      crud.aggregate[Student](
          "$match":> {"name":> "Alfredo"},
          "$match":> {"collegeUuid" :> student.collegeUuid},
          "$group":> {"_id" :> null <> {"count":> {"$sum":> 1}}},
          "$project" :> {"_id":> 0 <> "count":> 1}
      ) === List({"count":> 3})
      // if this fails, you might be running < mongo 2.3.x
    }
  }
}
