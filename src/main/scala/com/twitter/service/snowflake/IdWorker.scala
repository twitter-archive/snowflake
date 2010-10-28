/** Copyright 2010 Twitter, Inc.*/
package com.twitter.service.snowflake

import com.twitter.ostrich.Stats
import com.twitter.ostrich.W3CReporter
import com.twitter.service.snowflake.gen._
import net.lag.logging.Logger
import java.util.Random

/**
 * An object that generates IDs.
 * This is broken into a separate class in case
 * we ever want to support multiple worker threads
 * per process
 */
class IdWorker(workerId: Long, datacenterId: Long) extends Snowflake.Iface {
  private val log = Logger.get
  val genCounter = Stats.getCounter("ids_generated")
  val reporter = new Reporter
  val rand = new Random

  // Tue, 21 Mar 2006 20:50:14.000 GMT
  val twepoch = 1142974214000L

  var sequence = 0L
  val workerIdBits = 5
  val datacenterIdBits = 5
  val maxWorkerId = -1L ^ (-1L << workerIdBits)
  val maxDatacenterId = -1L ^ (-1L << datacenterIdBits)
  val sequenceBits = 12

  val workerIdShift = sequenceBits
  val datacenterIdShift = sequenceBits + workerIdBits
  val timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits
  val sequenceMask = -1L ^ (-1L << sequenceBits)

  var lastTimestamp = -1L

  // sanity check for workerId
  if (workerId > maxWorkerId || workerId < 0) {
    throw new IllegalArgumentException("worker Id can't be greater than %d or less than 0".format(maxWorkerId))
  }

  if (datacenterId > maxDatacenterId || datacenterId < 0) {
    throw new IllegalArgumentException("datacenter Id can't be greater than %d or less than 0".format(maxWorkerId))
  }

  log.info("worker starting. timestamp left shift %d, datacenter id bits %d, worker id bits %d, sequence bits %d, workerid %d",
    timestampLeftShift, datacenterIdBits, workerIdBits, sequenceBits, workerId)

  def get_id(useragent: String): Long = {
    if (!validUseragent(useragent)) {
      throw new InvalidUserAgentError
    }

    val id = nextId()

    reporter.report(new AuditLogEntry(id, useragent, rand.nextLong))
    id
  }

  def get_worker_id(): Long = workerId
  def get_datacenter_id(): Long = datacenterId
  def get_timestamp() = System.currentTimeMillis

  def nextId(): Long = synchronized {
    var timestamp = timeGen()

    if (lastTimestamp > timestamp) {
        log.warning("clock is moving backwards.  Rejecting requests until %d.", lastTimestamp);
        throw new InvalidSystemClock("Clock moved backwards.  Refusing to generate id for %d milliseconds".format(lastTimestamp - timestamp));
    }

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
      (datacenterId << datacenterIdShift) |
      (workerId << workerIdShift) | 
      sequence
  }

  def tilNextMillis(lastTimestamp: Long): Long = {
    var timestamp = timeGen()
    while (lastTimestamp == timestamp) {
      timestamp = timeGen()
    }
    timestamp
  }

  def timeGen(): Long = System.currentTimeMillis()

  val AgentParser = """([a-zA-Z][a-zA-Z\-0-9]*)""".r

  def validUseragent(useragent: String): Boolean = useragent match {
    case AgentParser(_) => true
    case _ => false
  }
}
