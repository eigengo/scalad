package org.cakesolutions.scalad.mongo

import collection.mutable
import collection.parallel.ThreadPoolTaskSupport
import annotation.tailrec
import concurrent.duration.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{LinkedBlockingQueue, ConcurrentLinkedQueue}
import java.util.concurrent.locks.ReentrantLock

trait Paging[T] {
  this: Iterator[T] =>

  /** Upgrades `Iterator`s to return results in batches of a given size.
    *
    * (Note that paging does imply anything on the buffering strategy)
    */
  def page(entries: Int)(f: List[T] => Unit) {
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

trait ParallelPaging[T] extends Paging[T] {
  this: Iterator[T] =>

  private val pool = new ThreadPoolTaskSupport

  def foreachpage(f: T => Unit, size: Int = 100) {
    page(size) {
      p =>
        val par = p.par
        par.tasksupport = pool
        par.foreach(i => f(i))
    }
  }
}

/** A very clean `Iterator` realisation of the
  * Producer / Consumer pattern where the producer and
  * consumer run in separate threads.
  *
  * Both the `hasNext` and `next` methods of the `Iterator`
  * may block (e.g. when the consumer catches up with the
  * producer).
  *
  * This is best used by a single producer and single
  * consumer but can be extended to multiple consumers
  * under the caveat that `next` may return `null` following
  * a successful `hasNext` (another thread
  * may have grabbed it first).
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
  def stop() {
    stopSignal set true
  }
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
  def produce(el: T)

  /** Finish producing.
    */
  def close()

  /** @return `true` if the consumer instructed the producer to stop.
    */
  def stopped() = stopSignal.get
}

abstract protected class AbstractProducerConsumer[T] extends ProducerConsumerIterator[T] {

  protected val queue: java.util.Queue[T]

  private val closed = new AtomicBoolean

  protected val lock = new ReentrantLock()

  protected val change = lock.newCondition()

  override def close() {
    lock lock()
    try {
      closed set true
      change signalAll()
    } finally
      lock unlock()
  }

  @tailrec
  override final def hasNext: Boolean =
    if (!queue.isEmpty) true
    else if (closed.get) !queue.isEmpty // non-locking optimisation
    else {
      lock lock()
      try {
          if (closed.get) return !queue.isEmpty
          change await()
      } finally
        lock unlock()
      hasNext
    }
}


/** Appropriate in cases where the producer is not expected to
  * create enough data to cause memory problems, regardless
  * of consumption rate.
  *
  * Has an effectively infinite buffer.
  */
final class NonblockingProducerConsumer[T] extends AbstractProducerConsumer[T] {

  // it may be cleaner to use mutable.SynchronizedQueue,
  // but ConcurrentLinkedQueue is both ridiculously efficient
  // and contains one of the greatest algorithms ever written.
  protected val queue = new ConcurrentLinkedQueue[T]()

  override def produce(el: T) {
    queue add el
    lock lock()
    try change signalAll()
    finally lock unlock()
  }

  override def next() = queue.poll()
}

/** Appropriate for memory constrained environments.
  *
  * Uses a finitely sized buffer to block the producer from adding
  * elements onto a `Queue` when the consumer is slow.
  *
  * Has an optional timeout on the block operation, at which point an
  * exception is raised from all `Iterator` methods. If no timeout
  * is used, the producer may block forever. If a zero timeout is given,
  * the buffer must never overflow (and the producer will never be
  * blocked).
  */
final class BlockingProducerConsumer[T](buffer: Int, timeout: Option[Duration] = None) extends AbstractProducerConsumer[T] {
  require(buffer > 0)

  protected val queue = new LinkedBlockingQueue[T](buffer)

  private val timedout = new AtomicBoolean

  override def produce(el: T) {
    if (timeout.isDefined) {
      val duration = timeout.get
      val taken = if (duration.length == 0) {
        queue.offer(el)
      } else
        queue.offer(el, duration.length, duration.unit)
      if (!taken)
        timedout set true
    }
    else
      queue.put(el)

    lock lock()
    try change signalAll()
    finally lock unlock()
    timeoutCheck()
  }

  private def timeoutCheck() {
    if (!stopped && timedout.get) throw new IllegalStateException(getClass + " timed out.")
  }

  override def next() = {
    timeoutCheck()
    queue.poll()
  }
}
