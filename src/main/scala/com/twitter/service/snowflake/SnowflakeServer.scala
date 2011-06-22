/** Copyright 2010-2011 Twitter, Inc. */
package com.twitter.service.snowflake

import com.twitter.service.snowflake.client.SnowflakeClient
import com.twitter.service.snowflake.gen._
import org.apache.thrift.{TException, TProcessor, TProcessorFactory}
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocol, TProtocolFactory}
import org.apache.thrift.transport._
import org.apache.thrift.server.{THsHaServer, TServer}
import com.twitter.zookeeper.ZooKeeperClient
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.data.{ACL, Id}
import org.apache.zookeeper.CreateMode._
import org.apache.zookeeper.{KeeperException, CreateMode, Watcher, WatchedEvent}
import org.apache.zookeeper.KeeperException.NodeExistsException
import scala.collection.mutable
import java.net.InetAddress
import com.twitter.ostrich.admin.RuntimeEnvironment
import com.twitter.ostrich.stats.Stats
import com.twitter.ostrich.admin.config.ServerConfig
import com.twitter.ostrich.admin.Service
import com.twitter.logging.Logger
import com.twitter.logging.config.LoggerConfig

trait ReporterConfig {
  val scribeCategory: String
  val scribeHost: String
  val scribePort: Int
  val scribeSocketTimeout: Int
  val flushQueueLimit: Int
}

trait SnowflakeConfig extends ServerConfig[SnowflakeServer] {
  val serverPort: Int
  val datacenterId: Int
  val workerId: Int
  val adminPort: Int
  val adminBacklog: Int
  val workerIdZkPath: String
  val zkHostlist: String
  val skipSanityChecks: Boolean
  val startupSleepMs: Int
  val thriftServerThreads: Int

  val reporterConfig: ReporterConfig

  val loggerConfig: LoggerConfig

  def apply(runtime: RuntimeEnvironment) = {
    new SnowflakeServer(this)
  }
}

case class Peer(hostname: String, port: Int)

object SnowflakeServer {
   def main(args: Array[String]) {
     val runtime = new RuntimeEnvironment(this)
     runtime.parseArgs(args.toList)

     val server = runtime.loadRuntimeConfig[SnowflakeServer]()
     try {
       server.start
     } catch {
       case e: Exception =>
         e.printStackTrace()
         println(e, "Unexpected exception: %s", e.getMessage)
         System.exit(0)
     }
   }
}

class SnowflakeServer(config: SnowflakeConfig) extends Service {
  Logger.configure(List(config.loggerConfig))
  private val log = Logger.get

  var server: TServer = null
  lazy val zkClient = {
    log.info("Creating ZooKeeper client connected to %s", config.zkHostlist)
    new ZooKeeperClient(config.zkHostlist)
  }

  Stats.addGauge("datacenter_id") { config.datacenterId }
  Stats.addGauge("worker_id") { config.workerId }

  def shutdown(): Unit = {
    if (server != null) {
      log.info("Shutting down.")
      server.stop()
      server = null
    }
  }

  def start {
    if (!config.skipSanityChecks) {
      sanityCheckPeers()
    }

    registerWorkerId(config.workerId)
    val admin = new AdminService(config.adminPort, config.adminBacklog, new RuntimeEnvironment(getClass))

    Thread.sleep(config.startupSleepMs)

    try {
      val worker = new IdWorker(config.workerId, config.datacenterId, config.reporterConfig)

      val processor = new Snowflake.Processor(worker)
      val transport = new TNonblockingServerSocket(config.serverPort)
      val serverOpts = new THsHaServer.Options
      serverOpts.workerThreads = config.thriftServerThreads

      val server = new THsHaServer(processor, transport, serverOpts)

      log.info("Starting server on port %s with workerThreads=%s", config.serverPort, serverOpts.workerThreads)
      server.serve()
    } catch {
      case e: Exception => {
        log.error(e, "Unexpected exception while initializing server: %s", e.getMessage)
        throw e
      }
    }
  }

  def registerWorkerId(i: Int):Unit = {
    log.info("trying to claim workerId %d", i)
    var tries = 0
    while (true) {
      try {
        zkClient.create("%s/%s".format(config.workerIdZkPath, i), 
          (getHostname + ':' + config.serverPort).getBytes(), EPHEMERAL)
        return
      } catch {
        case e: NodeExistsException => {
          if (tries < 2) {
            log.info("Failed to claim worker id. Gonna wait a bit and retry because the node may be from the last time I was running.")
            tries += 1
            Thread.sleep(30000)
          } else {
            throw e
          }
        }
      }
      log.info("successfully claimed workerId %d", i)
    }
  }

  def peers(): mutable.HashMap[Int, Peer] = {
    var peerMap = new mutable.HashMap[Int, Peer]
    try {
      zkClient.get(config.workerIdZkPath)
    } catch {
      case _ => {
        log.info("%s missing, trying to create it", config.workerIdZkPath)
        zkClient.create(config.workerIdZkPath, Array(), PERSISTENT)
      }
    }

    val children = zkClient.getChildren(config.workerIdZkPath)
    children.foreach { i =>
      val peer = zkClient.get("%s/%s".format(config.workerIdZkPath, i))
      val list = new String(peer).split(':')
      peerMap(i.toInt) = new Peer(new String(list(0)), list(1).toInt)
    }
    log.info("found %s children".format(children.length))

    return peerMap
  }

  def sanityCheckPeers() {
    var peerCount = 0
    val timestamps = peers().filter{ case (id: Int, peer: Peer) =>
      !(peer.hostname == getHostname && peer.port == config.serverPort)
    }.map { case (id: Int, peer: Peer) =>
      try {
        log.info("connecting to %s:%s".format(peer.hostname, peer.port))
        var (t, c) = SnowflakeClient.create(peer.hostname, peer.port, 1000)
        val reportedWorkerId = c.get_worker_id()
        if (reportedWorkerId != id) {
          log.error("Worker at %s:%s has id %d in zookeeper, but via rpc it says %d", peer.hostname, peer.port, id, reportedWorkerId)
          throw new IllegalStateException("Worker id insanity.")
        }

        val reportedDatacenterId = c.get_datacenter_id()
        if (reportedDatacenterId != config.datacenterId) {
          log.error("Worker at %s:%s has datacenter_id %d, but ours is %d", 
            peer.hostname, peer.port, reportedDatacenterId, config.datacenterId)
          throw new IllegalStateException("Datacenter id insanity.")
        }

        peerCount = peerCount + 1
        c.get_timestamp().toLong
      } catch {
        case e: TTransportException => {
          log.error("Couldn't talk to peer %s at %s:%s", config.workerId, peer.hostname, peer.port)
          throw e
        }
      }
    }

    if (timestamps.toSeq.size > 0) {
      val avg = timestamps.foldLeft(0L)(_ + _) / peerCount
      if (math.abs(System.currentTimeMillis - avg) > 10000) {
        log.error("Timestamp sanity check failed. Mean timestamp is %d, but mine is %d, " +
                  "so I'm more than 10s away from the mean", avg, System.currentTimeMillis)
        throw new IllegalStateException("timestamp sanity check failed")
      }
    }
  }

  def getHostname(): String = InetAddress.getLocalHost().getHostName()

}
