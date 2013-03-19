# ScalaD

Reuse [Spray JSON](http://github.com/spray/spray-json/) formatters as serialisers for [MongoDB](http://www.mongodb.org) persistence in Scala, and get access to a useful CRUD for performing simple searches.

ScalaD is an implicit heavy API: users are advised to re-read the Implicits chapter from [Odersky's Book](http://www.amazon.com/dp/0981531644) if feeling overwhelmed.


When all relevant marshallers and mongo settings are implicitly in scope, using ScalaD is as simple as:

```scala
val entity = ...

val crud = new MongoCrud

crud.create(entity)

val update = entity.copy(name = "Bar")
crud.updateFirst(update)

val morePopular = crud.searchAll("""{"count": {"$gte"": %s}}""" param(update.count))
```

However, anybody using this library is strongly encouraged to read the [MongoDB Documentation](http://docs.mongodb.org/manual/) as it is often necessary to get close to the raw queries to understand what is happening.

In particular, anybody wishing to use the [Aggregation Framework](http://docs.mongodb.org/manual/applications/aggregation/), or perform partial updates (`$set`), will have to revert to the Java API to a large extent (ScalaD provides some nice convenience conversions for doing this).

**We are aware that there is a lot of boilerplate involved in setting up Spray JSON and the Mongo implicits – we are hoping to automatically generate this code in a future release.**


## CollectionProvider – `CREATE`, `SEARCH`

Typically, a `case class` will be persisted to a MongoDB collection, we can do this with code like the following:

```scala
// with UuidMarshalling
// with SprayJsonSerialisers

case class MyCaseClass(id: UUID, name: String, count: Long)

// set up the Spray JSON Marshaller
implicit val MyCaseClassFormat = jsonFormat3(MyCaseClass)

// start up the Java Mongo database bridge
val db = new Mongo(...)

// tell ScalaD to associate a named collection to the case class
// and to add some indexes. A uniqueness constraint on `id` is added
// automatically by `SimpleSprayJsonCollection`
implicit val MyCaseClassProvider = new SimpleSprayJsonCollection[MyCaseClass, UUID](db, "entities") {
    override def indexFields = """{"name": -1}""" :: Nil // reverse order index
}

val crud = new MongoCrud
```

This setup will enable the functionality to use `crud.create` and `crud.search*` on the collection.

`crud.search*` takes in a `DBObject` as the query parameter. ScalaD provides a `String` pimp that allows easy creation of such queries:

```scala
import MongoQueries._

crud.searchFirst[MyCaseClass]("""{"name": %s}""" param("Foo"))
crud.searchAll[MyCaseClass]("""{"name": %s, "count": {"$lte": %s}}""" params("Foo", 1000))
crud.searchAll[MyCaseClass]("{}" toBson) // finds everything in the collection
```

However, additional methods of the CRUD are made available when further implicits are provided – we will document these in the following sections.


## IdentityQuery – `UPDATE`, `DELETE`, `FIND`


If we provide an implicit which tells MongoDB how to **find** a given entity in the database – based on an example of the entity – then we can perform `crud.update*`, `crud.createOrUpdate*` and `crud.delete*` operations (and `crud.find*`).

The traditional concept of database row (or document) identity is by an `id` field, as opposed to comparing all the fields. We create an implicit that enables `FIND` like this:

```scala
// the parameter to the instance is the name of the field
implicit val MyCaseClassIdentity = new SerialisedIdentityQueryBuilder[MyCaseClass, UUID]("id") {
  // avoid expensive runtime reflection by explicitly retrieving the value
  def id(entity: MyCaseClass) = entity.id
}

// which allows the following

crud.create(entity)
val update = entity.copy(name = "Bar")
crud.updateFirst(update)

crud.findFirst(entity) // finds 'update'

crud.deleteFirst(entity) // deletes 'update' because they match the same MongoDB query
```



More complicated composite identity keys can be defined using `MongoQuery` and falling back to the more basic `IdentityQueryBuilder`:

```scala
import MongoQueries._

implicit val CompositeIdentity = new IdentityQueryBuilder[MyOtherCaseClass] {
  def createIdQuery(entity: MyOtherCaseClass) = """{"id": %s, "name": %s}""" params(entity.id, entity.name)
}
```

which should be accompanied with a composite uniqueness constraint on the collection:

```scala
implicit val MyOtherCaseClassProvider = new IndexedCollectionSprayJson[MyOtherCaseClass] {
    override def getCollection = db.getCollection("other")
    override def uniqueFields = """{"id": 1, "name": 1}""" :: Nil
    override def indexFields = """{"id": 1}""" :: """{"name": 1}""" :: Nil // individual indexes still needed for key based lookups
}
```

If the update that you have made to an entity has changed one of the identity keys (and you still want to consider the object to be the same as the pre-changed version) then you can use `crud.update(old, new)`.


## FieldQuery

If we provide an implicit which tells MongoDB how to find a given entity – based on a key value – then we can perform `crud.read*` operations. For simple, and typesafe fields, this is by far the easiest way to get results from a MongoDB. Let's create a few for our case class:

```scala
// first, tell ScalaD how to use Spray JSON to serialise the query terms
implicit val StringSerialiser = new SprayJsonSerialisation[String]
implicit val UuidSerialiser = new SprayJsonSerialisation[Uuid]
implicit val LongSerialiser = new SprayJsonSerialisation[Long]

// now register the fields as keys
implicit val MyCaseClassUuidKey = new SerialisedFieldQueryBuilder[MyCaseClass, UUID]("id")
implicit val MyCaseClassStringKey = new SerialisedFieldQueryBuilder[MyCaseClass, String]("name")
implicit val MyCaseClassLongKey = new SerialisedFieldQueryBuilder[MyCaseClass, Long]("count")

// which allows the following

val sams = crud.readAll[MyCaseClass, String]("Sam")
val unpopular = crud.readAll[MyCaseClass, Long](0)
val entity2 = crud.readFirst[MyCaseClass, UUID](entity.id) match {
    case None => throw new WtfException()
    case Some(e) => e
}
```

Perceptive readers will note that the `READ` operations are based on types, not field names, and can therefore not be used safely if there are multiple fields with the same type in a case class. For this reason, we advise using [value classes](http://docs.scala-lang.org/overviews/core/value-classes.html) along with the supplied `SingleValueCaseClassFormat` Spray JSON marshaller.


## Result `ConsumerIterator`

All results are returned as a special `ConsumerIterator`, which returns results as they are discovered. It is safe to treat this as an `Iterator` for filtering purposes, but it is one-shot so convert it into a `List` (or similar) if you require to parse it several times. In addition, if you intend to break iteration early, you must remember to call `ConsumerIterator.close` to free resources – resources are cleaned up automatically if the iterator runs to the end of the search results.

The `ConsumerIterator` is part of a `ProducerConsumerIterator` which allows production and consumption to occur in separate threads. The producer uses a finite length buffer to minimise the risk of OutOfMemory errors for particularly large results.


## Special Types

Because we're using Spray JSON to do the marshalling, it means that only JSON compatible objects are naturally supported by ScalaD.

However, JSON is missing a few keys object types, such as: `Date`, `UUID` and any distinction between number types.

MongoDB BSON is also missing a few key object types, such as: `UUID`, `BigInt` and `BigDecimal`. Indeed, MongoDB treats numbers as primitive types and has no support for arbitrary precision numbers.


We provide JSON marshallers for `UuidMarshalling` and `DateMarshalling` which creates JSON marshalled forms of `UUID` and `Date` objects to look like this (so they can still be understood by endpoint clients)

```
{"$uuid": "550e8400-e29b-41d4-a716-446655440000"}
{"$date": "2013-02-04T17:51:35.479+0000"} // "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
```

the serialisation layer will ensure that these are saved as `BinData` and `DateFormat` accordingly.


If you want to use arbitrary precision numbers, we provide case classes (and Spray JSON marshallers) called `StringBigInt` and `StringBigDecimal` which marshall to `String`.


Be warned that although Spray JSON will correctly marshall raw `BigInt`s and `BigDecimal`s, MongoDB will silently drop the precision (ScalaD will detect this and create a log for every object that loses precision, so hopefully this is caught at development time).
