# Working
For now, I have immediate execution of statements (see `with Immediate`); I include mixins
for the different operations. I have `DDL` and `Iteratees`, which perform arbitrary SQL and
select data according to the given iteratee.

    val jdbc = new JDBC(dataSource) with DDL with Iteratees with Immediate

## Execute arbitrary statements
I can use the `DDL` trait to execute arbitrary SQL, without receiving any results

    jdbc.execute("create table USER (id INT PRIMARY KEY, version INT, name VARCHAR(200))")
    jdbc.execute("INSERT INTO USER (id, version, name) values (1, 1, 'foo')")
    jdbc.execute("INSERT INTO USER (id, version, name) values (2, 1, 'bar')")
    jdbc.execute("INSERT INTO USER (id, version, name) values (3, 1, 'baz')")

## Execute select query
Prepare a `Statement`, set parameters, get `ResultSet`; iterate and return. Map all rows to some
objects and return whatever the iteratee specifies

    jdbc.select("select * from USER", list[User]) {rs => new User()}
    jdbc.select("select * from USER", head[User] >>= (u => head map (u2 => (u <|*|> u2)))) {rs=>new User()}

# Not quite there yet
I would like to add support for templated operations using arbitrary objects

    jdbc.insert("USER (id, name) values (:id, :name)" | u)
    jdbc.update("USER set name = :name where id = :id" | u)

In the code above, the instance u has accessible getters or fields `id` and `name`

## Precompiled statements
Particularly in high-throughput applications, I might want to use a precompiled
`Statement` and have the various functions from the operations mixins return an
instance that I can apply multiple times to the same `Statement`:

    val precompiled = new JDBC(dataSource) with Precompiled with Iteratees
    val byId = precompiled.select("SELECT * FROM USER where id=?", head[User]) {rs=>new User()}

    byId(1L)
    byId(2L)

The `byId` is _some instance_ that can be applied to the query parameters, and it returns
whatever the immediate query would return. In the example above, the result of immediate
execution of

    ... with Immediate
    select("SELECT * FROM USER where id=?" | 1L, head[User]) {rs=>new User()}

is `Option[User]`; the result of precompiled execution of

    ... with Precompiled
    select("SELECT * FROM USER where id=?", head[User]) {rs=>new User()}

notice that the precompiled version does not take parameters until you apply
the returned object, which is `PrecompiledStatement[Option[User]]` and can be applied
to arbitrary arguments:

    class PrecompiledStatement[R] {
       def apply(values: Any*): R
    }

So far, I have a proper type system of `ExecutionPolicy`:

    trait ExecutionPolicy {
      type Result[R]

      def exec[A](...): Result[A]
    }

    trait Immediate extends ExecutionPolicy {
      type Result[R] = R

      def exec[A](...) = a
    }

    trait Precompiled extends ExecutionPolicy {
      type Result[R] = PrecompiledStatement[R]

      def exec[A](...) = new PrecompiledStatement[A](a)
    }

I can also have what I call partially applied SQL with the `Precompiled` trait, allowing me
to specify the query's parameters partially at declaration and supply the rest at application:

    val q = jdbc.select("SELECT * FROM USER WHERE id=? and name like ? | 1L,
                        head[User]){rs=>new User()}

The variable `q` is now precompiled partially applicable query. To run, I must apply it to the
remaining parameter, like so:

    q("Jan")
    q("Anirvan")

The application returns `Option[User]` obtained by running `SELECT * FROM USER WHERE id=1 and name='Jan'` and
`SELECT * FROM USER WHERE id=1 and name='Anirvan'`, respectively.
