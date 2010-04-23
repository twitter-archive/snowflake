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
class IdWorker(workerId: Long) extends Snowflake.Iface {
  private val log = Logger.get
  var genCounter = Stats.getCounter("ids_generated")

  // Tue, 21 Mar 2006 20:50:14.000 GMT
  val twepoch = 1142974214000L

  var sequence = 0L
  // the number of bits used to record the worker Id
  val workerIdBits = 10
  val maxWorkerId = -1L ^ (-1L << workerIdBits)
  // the number of bits used to record the sequence
  val sequenceBits = 12

  val timestampLeftShift = sequenceBits + workerIdBits
  val workerIdShift = sequenceBits
  val sequenceMask = 4095 // -1L ^ (-1L << sequenceBits)

  var lastTimestamp = -1L

  // sanity check for workerId
  if (workerId > maxWorkerId || workerId < 0) {
    throw new IllegalArgumentException("worker Id can't be greater than %d or less than 0".format(maxWorkerId))
  }

  log.info("worker starting. timestamp left shift %d, worker id bits %d, sequence bits %d, workid %d",
    timestampLeftShift, workerIdBits, sequenceBits, workerId)

  def get_id(): Long = nextId
  def get_worker_id(): Long = workerId
  def get_timestamp() = System.currentTimeMillis


  def nextId(): Long = synchronized {
    var timestamp = timeGen()

    if (lastTimestamp == timestamp) {
      sequence = (sequence + 1) & sequenceMask
      if (sequence == 0) {
        timestamp = tilNextMillis(lastTimestamp)
      }
    } else {
      sequence = 0
    }

    lastTimestamp = timestamp
    genCounter.incr()
    ((timestamp - twepoch) << timestampLeftShift) |
    (workerId << workerIdShift) | sequence
  }

  def tilNextMillis(lastTimestamp:Long):Long = {
    var timestamp = timeGen()
    while (lastTimestamp == timestamp) {
      timestamp = timeGen()
    }
    timestamp
  }

  def timeGen():Long = System.currentTimeMillis()
}
