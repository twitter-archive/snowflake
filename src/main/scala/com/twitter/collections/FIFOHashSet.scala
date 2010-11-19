/** Copyright 2010 Twitter, Inc.*/
package com.twitter.collections

import scala.collection.mutable.LinkedHashSet

/**
 * A size-limited HashSet. Eviction is FIFO
 */
class FIFOHashSet[T](sizeLimit: Int) extends LinkedHashSet[T] {

  def this() {
    this(Integer.MAX_VALUE)
  }

  override def add(t: T): Boolean = {
    if (size >= sizeLimit) {
      trim
    }
    super.add(t)
  }

  protected def trim() {
    val iter = elements
    while (size >= sizeLimit) {
      remove(iter.next)
    }
  }
}