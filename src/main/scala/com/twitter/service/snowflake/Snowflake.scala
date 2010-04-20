/** Copyright 2010 Twitter, Inc.*/
package com.twitter.service.snowflake

import com.twitter.ostrich.Stats
import com.twitter.service.snowflake.gen._
import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import net.lag.logging.Logger

/**
 * Implementation of StatusService Thrift interface delegating to the
 * StatusStore subclass.
 */
class Snowflake(workerId: Long) extends Snowflake.Iface {
  val log = Logger.get
  val worker = new IdWorker(workerId)
  def get_id(): Long = worker.nextId
  def get_worker_id(): Long = workerId
  def get_timestamp() = System.currentTimeMillis
}
