/** Copyright 2010-2011 Twitter, Inc. */
package com.twitter.service.snowflake.client

import com.twitter.service.snowflake.gen.Snowflake
import com.twitter.service.snowflake.gen.Snowflake.Client
import org.apache.commons.pool.impl.{GenericObjectPool, StackKeyedObjectPoolFactory}
import org.apache.commons.pool.BasePoolableObjectFactory
import org.apache.thrift.transport.TTransport
import java.util.concurrent.ConcurrentHashMap
import com.twitter.logging.Logger


object ConnectionPool {
  private val poolMap: ConcurrentHashMap[String, ConnectionPool] = new ConcurrentHashMap[String, ConnectionPool]()
  private val log = Logger.get //TODO thread this in

  def clear() {
    poolMap.clear()
    log.debug("poolMap has been cleared")
  }

  def apply(hostname: String, port: Int): ConnectionPool = {
    // Looks into the Map of ConnectionPools, returns the right pool for the
    // hostname:port or else creates one
    val key = makeKey(hostname, port)
    var pool = poolMap.get(key)

    if (pool != null) {
      log.debug("found ConnectionPool in poolMap for %s".format(key))
      pool
    } else {
      pool = new ConnectionPool(hostname, port)
      log.debug("adding new ConnectionPool to poolMap after missing lookup for %s".format(key))
      poolMap.putIfAbsent(key, pool)
      pool
    }
  }

  def reset(maxActive: Int, hostname: String, port: Int): ConnectionPool = {
    poolMap.remove(makeKey(hostname, port))
    apply(hostname, port)
  }

  private def makeKey(hostname: String, port: Int): String = "%s:%d".format(hostname, port)
}

class ThriftConnectionFactory(hostname: String, port: Int) extends BasePoolableObjectFactory {
  private val log = Logger.get
  // for makeObject we'll simply return a new buffer
  def makeObject() = {
    log.debug("creating new Thrift Client")
    ThriftClientManager(hostname, port)
  }

  override def validateObject(obj: Object): Boolean = {
    val (transport, snowflakeClient) = obj.asInstanceOf[Tuple2[TTransport, Snowflake.Client]]
    try {
      if (!transport.isOpen) transport.open()
      log.info("ping succeeded")
      snowflakeClient.get_timestamp > 0
    } catch {
      case e => {
        log.info(e, "connection pool validation failed: ping failed to doc server")
        transport.close()
        false
      }
    }
  }
}

class ConnectionPool(hostname: String, port: Int) {
  private val log = Logger.get

  def getObjectPoolConfig: GenericObjectPool.Config = {
    val config = new GenericObjectPool.Config
    config
  }

  // needs to be non-private for testing, but don't access it
  // and expect it to work. use withClient or withConnection.
  def thriftConnectionFactory = new ThriftConnectionFactory(hostname, port)

  private[client] lazy val objectPool = makeObjectPool

  private[client] def makeObjectPool = {
    log.info("creating GenericObject pool")
    val objPool = new GenericObjectPool(thriftConnectionFactory, getObjectPoolConfig)
    objPool.setTestWhileIdle(false)
    objPool.setTestOnReturn(true)
    objPool
  }

  def borrowObject: (TTransport, Snowflake.Client) = {
    objectPool.borrowObject.asInstanceOf[Tuple2[TTransport, Snowflake.Client]]
  }

  private def connection: (TTransport, Snowflake.Client) = {
    borrowObject
  }

  def withClient[A](f: (Snowflake.Client) => A): A = {
    withConnection { transportClient =>
      val (transport, client: Snowflake.Client) = transportClient
      if (!transport.isOpen) transport.open()
      f(client)
    }
  }

  def withConnection[A](f: ((TTransport, Snowflake.Client)) => A): A = {
    log.ifDebug("Borrowing from connection pool: Currently: numActive = %d numIdle = %d maxActive = %d".
                format(objectPool.getNumActive, objectPool.getNumIdle, objectPool.getMaxActive))

    val conn = connection // get a connection for this operation

    try {
      f(conn)
    } catch {
      case e: Exception => {
        log.error("Error talking to Thrift Server: %s with msg: %s", this.toString, e.getMessage)
        null.asInstanceOf[A]
      }
    } finally {
      log.debug("going to return object")
      objectPool.returnObject(conn)
      log.debug("object returned to pool")
    }
  }

  override def toString = hostname + ":" + port
}
