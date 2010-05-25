/** Copyright 2010 Twitter, Inc. */
package com.twitter.service.snowflake

import com.twitter.service.snowflake.client.SnowflakeClient
import com.twitter.service.snowflake.gen._
import org.apache.thrift.{TException, TProcessor, TProcessorFactory}
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocol, TProtocolFactory}
import org.apache.thrift.transport._
import org.apache.thrift.server.{THsHaServer, TServer, TThreadPoolServer}
import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import net.lag.logging.Logger
import scala.tools.nsc.MainGenericRunner
import com.twitter.zookeeper.client._
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.data.{ACL, Id}
import org.apache.zookeeper.CreateMode._
import org.apache.zookeeper.KeeperException
import scala.collection.mutable
import scala.util.Sorting
import java.net.InetAddress

object SnowflakeServer {
  private val log = Logger.get
  val runtime = new RuntimeEnvironment(getClass)
  var server: TServer = null
  var workerId: Int = -1
  val workers = new mutable.ListBuffer[IdWorker]()
  lazy val PORT = Configgy.config("snowflake.server_port").toInt
  lazy val zkPath = Configgy.config("zookeper_worker_id_path")
  lazy val zkWatcher = new FakeWatcher
  lazy val hostlist = Configgy.config("zookeeper-client.hostlist")
  lazy val zkClient = {
    log.info("Creating ZooKeeper client connected to %s", hostlist)
    new ZookeeperClient(zkWatcher, hostlist, Configgy.config)
  }

  def shutdown(): Unit = {
    if (server != null) {
      log.info("Shutting down.")
      server.stop()
      server = null
    }
  }

  def main(args: Array[String]) {
    runtime.load(args)

    if (!Configgy.config("snowflake.skip_sanity_checks").toBoolean) {
      sanityCheckPeers()
    }

    loadWorkerId()
    val admin = new AdminService(Configgy.config, runtime)

    // TODO we should sleep for at least as long as our time-drift SLA
    Thread.sleep(Configgy.config("snowflake.startup_sleep_ms").toLong)

    try {
      val worker = new IdWorker(workerId)
      workers += worker
      log.info("snowflake.server_port loaded: %s", PORT)

      val transport = new TNonblockingServerSocket(PORT)
      val processor = new Snowflake.Processor(worker)
      val protoFactory = new TBinaryProtocol.Factory(true, true)

      val serverOpts = new THsHaServer.Options
      serverOpts.minWorkerThreads = Configgy.config.getInt("snowflake.thrift-server-threads-min", 200)
      serverOpts.maxWorkerThreads = Configgy.config.getInt("snowflake.thrift-server-threads-max", 800)

      val server = new THsHaServer(processor, transport, serverOpts)

      log.info("Starting server on port %s", PORT)
      server.serve()
    } catch {
      case e: Exception => {
        log.error(e, "Unexpected exception while initializing server: %s", e.getMessage)
        throw e
      }
    }
  }

  def loadWorkerId() {
    workerId = Configgy.config("worker_id", -1)

    while (workerId < 0) {
      try {
        val p = peers()
        val children = p.keys.collect.toArray
        Sorting.quickSort(children)
        val id = findFirstAvailableId(children)

        log.info("trying to claim workerId %d", id)
        zkClient.create("%s/%s".format(zkPath, id), getHostname.getBytes(), Ids.OPEN_ACL_UNSAFE, EPHEMERAL)
        log.info("successfully claimed workerId %d", id)
        workerId = id;
      } catch {
        case e: KeeperException => log.info("workerId collision, retrying")
      }
    }
  }

  def peers(): mutable.HashMap[Int, String] = {
    var peerMap = new mutable.HashMap[Int, String]
    try {
      zkClient.get(zkPath)
    } catch {
      case _ => {
        log.info("%s missing, trying to create it", zkPath)
        zkClient.create(zkPath, Array(), Ids.OPEN_ACL_UNSAFE, PERSISTENT)
      }
    }

    val children = zkClient.getChildren(zkPath)
    children.foreach { i =>
      val hostname = zkClient.get("%s/%s".format(zkPath, i))
      peerMap(i.toInt) = new String(hostname)
    }
    log.info("found %s children".format(children.length))

    return peerMap
  }

  def sanityCheckPeers() {
    var peerCount = 0L
    val timestamps = peers().map { case (workerId: Int, hostname: String) =>
      try {
        log.info("connecting to %s:%s".format(hostname, PORT))
        var (t, c) = SnowflakeClient.create(hostname, PORT, 1000)
        val reportedWorkerId = c.get_worker_id().toLong
        if (reportedWorkerId != workerId) {
          log.error("Worker at %s has id %d in zookeeper, but via rpc it says %d", hostname, workerId, reportedWorkerId)
          throw new IllegalStateException("Worker id insanity.")
        }
        c.get_timestamp().toLong
      } catch {
        case e: TTransportException => {
          log.error("Couldn't talk to peer %s at %s", workerId, hostname)
          throw e
        }
      }
    }

    if (timestamps.toSeq.size > 0) { // only run if peers exist
      val avg = timestamps.foldLeft(0L)(_ + _) / peerCount
      if (Math.abs(System.currentTimeMillis - avg) > 10000) {
        log.error("Timestamp sanity check failed. Mean timestamp is %d, but mine is %d, " +
                  "so I'm more than 10s away from the mean", avg, System.currentTimeMillis)
        throw new IllegalStateException("timestamp sanity check failed")
      }
    }
  }

  def getHostname(): String = InetAddress.getLocalHost().getHostName()

  def findFirstAvailableId(children: Array[Int]): Int = {
    if (children.length > 1) {
      for (i <- 1 until children.length) {
        if (children(i) > (children(i - 1) + 1)) {
          return children(i-1) + 1
        }
      }
      return children.last + 1
    } else if (children.length == 1 && children.first == 0) {
      1
    } else {
      0
    }
  }
}
