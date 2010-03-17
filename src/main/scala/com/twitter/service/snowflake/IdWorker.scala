/** Copyright 2009 Twitter, Inc.*/
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
  // used to truncate timestamp into appropriate number of bits
  // defaulting to 9 gives us roughly 1-second resolution
  val timestampRightShift = 10
  // the number of bits used to record the timestamp
  val timestampBits = 32
  // the number of bits used to record the worker Id
  val workerIdBits = 8
  val maxWorkerId = -1L ^ (-1L << workerIdBits)
  // the number of bits used to record the sequence
  val sequenceBits = 24

  val timestampLeftShift = sequenceBits + workerIdBits
  val workerIdShift = sequenceBits
  val sequenceMask = -1L ^ (-1L << sequenceBits)

  // sanity check for workerId
  if (workerId > maxWorkerId) {
    throw new IllegalArgumentException("worker Id can't be greater than %d".format(maxWorkerId))
  }

  log.info("worker starting.  timestamp right shift %d, timestamp left shift %d, worker id bits %d, sequence bits %d",
    timestampRightShift, timestampLeftShift, workerIdBits, sequenceBits)


  def nextId(): Long = {
    nextId(System.currentTimeMillis)
  }

  def nextId(timestamp: Long): Long = {
    synchronized {
      // let this wrap indefinitely.  
      // At 24 sequence bits this gives us 16 million ids per timestamp increment
      sequence += 1
      genCounter.incr()
      ((timestamp >> timestampRightShift) << timestampLeftShift) |
        (workerId << workerIdShift) | (sequence & sequenceMask)
    }
  }

}
