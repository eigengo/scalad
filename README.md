# ScalaD

Reuse [Spray JSON](http://github.com/spray/spray-json/) formatters as serialisers for [MongoDB](http://www.mongodb.org) persistence in Scala, and get access to a useful CRUD for performing simple searches.

ScalaD is an implicit heavy API: users are advised to re-read the Implicits chapter from [Odersky's Book](http://www.amazon.com/dp/0981531644) if feeling overwhelmed.


When all relevant marshallers and mongo settings are implicitly in scope, using ScalaD is as simple as:

```scala
val entity = ...

val crud = new SprayMongo

crud.create(entity)

val update = entity.copy(name = "Bar")
crud.findAndReplace(entity, update)

val morePopular = crud.searchAll("count":> {"$gte":> update.count})  // awesome DSL for JSON
```

However, anybody using this library is strongly encouraged to read the [MongoDB Documentation](http://docs.mongodb.org/manual/) as it is often necessary to get close to the raw queries to understand what is happening.

In particular, anybody wishing to use the [Aggregation Framework](http://docs.mongodb.org/manual/applications/aggregation/), or perform partial updates (`$set`), will have to revert to the Java API to a large extent (ScalaD provides some nice convenience conversions for doing this).

**We are aware that there is a lot of boilerplate involved in setting up Spray JSON implicits – we are hoping to automatically generate this code in a future release.**

The best place to find more examples are the specs and the akka-patterns project:

* [PersistenceSpec.scala](src/test/scala/org/cakesolutions/scalad/mongo/sprayjson/PersistenceSpec.scala)
* [Akka Patterns](https://github.com/janm399/akka-patterns)


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
