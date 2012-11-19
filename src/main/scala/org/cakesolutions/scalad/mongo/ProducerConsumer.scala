package org.cakesolutions.scalad.mongo

import concurrent.Lock
import collection.mutable
import java.util.concurrent.atomic.AtomicBoolean
import annotation.tailrec
import java.util.concurrent.{ConcurrentLinkedQueue, TimeUnit}

trait Paging[T] {
  this: Iterator[T] =>

  /** Upgrades `Iterator`s to return results in batches of a given size.
    *
    * (Note that paging does imply anything on the buffering strategy)
    */
  def page(entries: Int)(f: List[T] => Unit): Unit = {
    require(entries > 1)
    val buffer = new mutable.ListBuffer[T]
    while (hasNext) {
      buffer append next()
      if (buffer.size % entries == 0) {
        f(buffer.toList)
        buffer.clear()
      }
    }
    if (!buffer.isEmpty) f(buffer.toList)
  }
}

/** A very clean `Iterable` realisation of the
  * Producer / Consumer pattern.
  *
  * Both the `hasNext` and `next` methods of the `Iterator`
  * may block (e.g. when the consumer catches up with the
  * producer).
  *
  * If the client wishes to cancel iteration early, the
  * `stop` method may be called to free up resources.
  *
  * Functional purists may use this in their `Iteratees`
  * patterns.
  *
  * This is a multi-threaded alternative to the
  * [[http://en.wikipedia.org/wiki/Coroutine co-routine]]
  * pattern.
  *
  *
  * It is a common misconception that `Iterator.hasNext` is
  * not allowed to block.
  * However, the API documentation does not preclude
  * blocking behaviour. Indeed, the
  * Scala standard library encourages consumer blocking in
  * the XML Pull API: [[scala.xml.pull.ProducerConsumerIterator]].
  */
trait ConsumerIterator[T] extends Iterator[T] with Paging[T] {

  protected val stopSignal = new AtomicBoolean

  /** Instruct the implementation to truncate at its
    * earliest convenience and dispose of resources.
    */
  def stop() = stopSignal set true
}

/** The producer's side of
  * [[org.cakesolutions.scalad.mongo.ConsumerIterator]].
  *
  * Implementations should extend this and be thread safe for
  * multiple producer threads and may assume a single
  * consumer thread.
  */
trait ProducerConsumerIterator[T] extends ConsumerIterator[T] {

  /** Make an element available for the consumer.
    */
  def produce(el: T): Unit

  /** Finish producing.
    */
  def close(): Unit

  /** @return `true` if the consumer instructed the producer to stop.
    */
  def stopped() = stopSignal.get
}

/** Appropriate in cases where the producer is not expected to
  * create enough data to cause memory problems, regardless
  * of consumption rate.
  *
  * Has an effectively infinite buffer.
  */
final class NonblockingProducerConsumer[T] extends ProducerConsumerIterator[T] {

  // ?? is there a more "Scala" way to use wait/notify

  // blocker used in hasNext, notify on changes
  private val blocker = new AnyRef
  // lock used to avoid a race condition on close
  private val lock = new Lock

  // it may be cleaner to use mutable.SynchronizedQueue,
  // but ConcurrentLinkedQueue is both ridiculously efficient
  // and contains one of the greatest algorithms ever written.
  private val queue = new ConcurrentLinkedQueue[T]()

  private val closed = new AtomicBoolean

  override def produce(el: T) {
    queue add el
    blocker.synchronized(blocker.notify())
  }

  override def close() {
    lock acquire()
    closed set true
    lock release()
    blocker.synchronized(blocker.notify())
  }

  override def stop() {
    super.stop()
    blocker.synchronized(blocker.notify())
  }

  @tailrec
  override def hasNext =
    if (!queue.isEmpty) true
    else if (closed.get) !queue.isEmpty // non-locking optimisation
    else {
      lock.acquire()
      if (closed.get) {
        // avoids race condition with 'close'
        lock.release()
        !queue.isEmpty
      } else {
        lock.release()
        blocker.synchronized(blocker.wait()) // will block until more is known
        hasNext
      }
    }

  override def next() = queue.poll()
}

/** Appropriate for highly memory constrained
  * environments where it is reasonable to block the producer
  * to save memory use.
  *
  * Uses a finitely sized buffer to block the producer from adding
  * elements onto a `Queue` when the consumer is slow.
  *
  * Has an optional timeout on the block operation, at which point an
  * exception is raised from all `Iterator` methods. If no timeout
  * is used, the producer may block forever.
  */
final class BlockingProducerConsumer[T](timeout: Option[(Long, TimeUnit)]) extends ProducerConsumerIterator[T] {
  require(timeout.isEmpty || timeout.get._1 > 0)

  def hasNext = ???

  def next() = ???

  def produce(el: T) = ???

  def close() = ???
}

/** Appropriate for environments
  * where the producer should not be blocked and slow
  * consumers are not tolerated.
  *
  * Uses a finite buffer to store elements which are added
  * to a `Queue`. Rather than block, the producer will throw exceptions.
  *
  * This may be viewed as a more rigorously defined version of
  * [[org.cakesolutions.scalad.mongo.BlockingProducerConsumer]],
  * since this does not require an arbitrary timeout value and is
  * defined entirely in terms of the number of storage elements.
  */
final class FiniteProducerConsumer[T](buffer: Int) extends ProducerConsumerIterator[T] {
  require(buffer > 0)

  def hasNext = ???

  def next() = ???

  def produce(el: T) = ???

  def close() = ???
}
