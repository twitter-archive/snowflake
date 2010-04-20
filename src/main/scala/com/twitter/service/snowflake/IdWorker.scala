/** Copyright 2010 Twitter, Inc.*/
package com.twitter.service.snowflake

import com.twitter.ostrich.Stats
import com.twitter.service.snowflake.gen._
import net.lag.logging.Logger
import scala.actors.Actor
import scala.actors.Actor._

/**
 * An object that generates IDs.  
 * This is broken into a separate class in case
 * we ever want to support multiple worker threads
 * per process
 */
class IdWorker(workerId: Long) {
  private val log = Logger.get
  var genCounter = Stats.getCounter("ids_generated")
  var sequence = 0L
  val twepoch = 1142974214000L
  // the number of bits used to record the timestamp
  val timestampBits = 42
  // the number of bits used to record the worker Id
  val workerIdBits = 10
  val maxWorkerId = -1L ^ (-1L << workerIdBits)
  // the number of bits used to record the sequence
  val sequenceBits = 12

  val timestampLeftShift = sequenceBits + workerIdBits
  val workerIdShift = sequenceBits
  val sequenceMask = -1L ^ (-1L << sequenceBits)

  var lastTimestamp = -1L

  // sanity check for workerId
  if (workerId > maxWorkerId) {
    throw new IllegalArgumentException("worker Id can't be greater than %d".format(maxWorkerId))
  }

  log.info("worker starting.  timestamp left shift %d, worker id bits %d, sequence bits %d",
    timestampLeftShift, workerIdBits, sequenceBits)


  def nextId(): Long = {
    nextId((() => System.currentTimeMillis))
  }

  def nextId(timeGen: (() => Long)): Long = {
    synchronized {
      sequence = (sequence + 1) & sequenceMask
      var timestamp = timeGen()
      if (sequence == 0) {
        while (lastTimestamp == timestamp) {
          sleeper()
          timestamp = timeGen()
        }
        lastTimestamp = timestamp
        sequence = 0
      }
      genCounter.incr()
      ((timestamp - twepoch) << timestampLeftShift) |
        (workerId << workerIdShift) | sequence
    }
  }

  def sleeper() = {
    Thread.sleep(1)
  }
}
