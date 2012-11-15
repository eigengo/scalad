package org.cakesolutions.scalad.mongo

import concurrent.Lock
import collection.mutable
import java.util.concurrent.atomic.AtomicBoolean
import annotation.tailrec

/**
 * Implementation that uses a `Queue` to buffer the results
 * of an operation, blocking on `hasNext`. `next` will
 * not block if `hasNext` is `true`.
 */
class ProducerConsumerIterator[T] extends ConsumerIterator[T] {

  // ?? is there a more "Scala" way to use wait/notify

  // blocker used in hasNext, notify on changes
  private val blocker = new AnyRef
  // lock used to avoid a race condition on close
  private val lock = new Lock

  private val queue = new mutable.SynchronizedQueue[T]
  private val stopSignal = new AtomicBoolean
  private val closed = new AtomicBoolean

  def push(el: T) {
    queue.enqueue(el)
    blocker.synchronized(blocker.notify())
  }

  def stopped() = stopSignal.get

  def close() {
    lock.acquire()
    closed.set(true)
    lock.release()
    blocker.synchronized(blocker.notify())
  }

  override def stop() {
    stopSignal set true
    blocker.synchronized(blocker.notify())
  }

  @tailrec
  override final def hasNext =
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

  override def next() = queue.dequeue()
}
