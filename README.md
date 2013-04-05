# ScalaD

Reuse [Spray JSON](http://github.com/spray/spray-json/) formatters as serialisers for [MongoDB](http://www.mongodb.org) persistence in Scala, and get access to a useful CRUD for performing simple searches.

ScalaD is an implicit heavy API: users are advised to re-read the Implicits chapter from [Odersky's Book](http://www.amazon.com/dp/0981531644) if feeling overwhelmed.


When all relevant marshallers and mongo settings are implicitly in scope, using ScalaD is as simple as:

```scala
val entity = ...

val crud = new SprayMongo

crud.insert(entity)
crud.findAndUpdate("id":>entity.id, "$set":>{"name":>"Bar"})
val update = crud.findOne("id":>entity.id)

val popular = crud.find("count":> {"$gte":> update.count})  // awesome DSL for JSON
```

However, anybody using this library is strongly encouraged to read the [MongoDB Documentation](http://docs.mongodb.org/manual/) as it is often necessary to get close to the raw queries to understand what is happening, especially the [Aggregation Framework](http://docs.mongodb.org/manual/applications/aggregation/).

The best place to find more examples are the specs and the akka-patterns project:

* [PersistenceSpec.scala](src/test/scala/org/cakesolutions/scalad/mongo/sprayjson/PersistenceSpec.scala)
* [Akka Patterns](https://github.com/janm399/akka-patterns)

## Dependencies

Add the dependency to your build file. In SBT, write

```scala
"org.eigengo" % "scalad" %% "1.3.0-EG"
```

or, if you must use Maven, write

```xml
<dependency>
  <groupId>org.eigengo</groupId>
  <artifactId>scalad_2.10</artifactId>
  <version>1.3.0-EG</version>
</dependency>
```

## Special Types

Because we're using Spray JSON to do the marshalling, it means that only JSON compatible objects are naturally supported by ScalaD.

However, JSON is missing a few key object types, such as: `Date`, `UUID` and any distinction between number types.

MongoDB BSON is also missing a few key object types, such as: `UUID`, `BigInt` and `BigDecimal`. Indeed, MongoDB treats numbers as primitive types and has no support for arbitrary precision numbers.


We provide JSON marshallers for `UuidMarshalling` and `DateMarshalling` which creates JSON marshalled forms of `UUID` and `Date` objects to look like this (so they can still be understood by endpoint clients)

```
{"$uuid": "550e8400-e29b-41d4-a716-446655440000"}
{"$date": "2013-02-04T17:51:35.479+0000"} // "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
```

the serialisation layer will ensure that these are saved as `BinData` and `DateFormat` accordingly.


If you want to use arbitrary precision numbers, we provide case classes (and Spray JSON marshallers) called `StringBigInt` and `StringBigDecimal` which marshall to `String`. Hopefully Spray JSON will address this magically with a fix to their [issue #44](https://github.com/spray/spray-json/issues/44).


Be warned that although Spray JSON will correctly marshall raw `BigInt`s and `BigDecimal`s, MongoDB will silently drop the precision (ScalaD will detect this and create a log for every object that loses precision, so hopefully this is caught at development time).
