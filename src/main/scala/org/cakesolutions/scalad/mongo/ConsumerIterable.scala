package org.cakesolutions.scalad.mongo

/**
 * An `Iterable` that provides a very clean interface to the
 * Producer / Consumer pattern.
 *
 * TODO: shortly after creating this, we spotted
 * [[scala.xml.pull.ProducerConsumerIterator]]. We will
 * investigate the merits of our approach and if we are
 * duplicating functionality, our Iterable classes will be dropped.
 *
 * Both the `hasNext` and `next` methods of the `Iterator`
 * may block if the consumer catches up with the producer.
 *
 * If the client wishes to cancel iteration early, the
 * `stop` method may be called to free up resources. Some
 * implementations may introduce a timeout feature which
 * will automatically free or log unclosed resources on
 * idle activity.
 *
 * Functional purists may use this in their `Iteratees`
 * patterns.
 *
 *
 * It is a common misconception that `Iterator.hasNext` is
 * not allowed to block.
 * However, the API documentation does not preclude
 * blocking behaviour. Note that `Queue` implementations
 * return `false` once the consumer reaches the tail – a
 * fundamental difference opposite this trait.
 */
trait ConsumerIterator[T] extends Iterator[T] {

  /**
   * Instruct the backing implementation to truncate at its
   * earliest convenience and dispose of resources.
   */
  def stop()

  /**
   * Callback which limits the number of entities in each call
   * to no more than the given parameter.
   *
   * (Note that paging does imply anything on the buffering strategy,
   * which decides how many entries to store in memory from a search
   * on the database.)
   */
  def page(entries: Int)(f: List[T] => Unit): Unit = ???
}
