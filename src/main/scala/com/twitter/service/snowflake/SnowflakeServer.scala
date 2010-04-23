/** Copyright 2010 Twitter, Inc. */
package com.twitter.service.snowflake

import com.twitter.service.snowflake.gen._
import com.twitter.ostrich.W3CStats
import org.apache.thrift.TException
import org.apache.thrift.TProcessor
import org.apache.thrift.TProcessorFactory
import org.apache.thrift.protocol.TProtocol
import org.apache.thrift.protocol.TProtocolFactory
import org.apache.thrift.transport.TNonblockingServerSocket
import org.apache.thrift.transport.TServerTransport
import org.apache.thrift.transport.TServerSocket
import org.apache.thrift.transport.TTransport
import org.apache.thrift.transport.TTransportFactory
import org.apache.thrift.transport.TTransportException
import org.apache.thrift.server.THsHaServer
import org.apache.thrift.server.TServer
import org.apache.thrift.server.TThreadPoolServer
import org.apache.thrift.protocol.TBinaryProtocol
import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import net.lag.logging.Logger
import scala.tools.nsc.MainGenericRunner
import com.twitter.zookeeper.client._
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.data.{ACL, Id}
import org.apache.zookeeper.CreateMode._
import org.apache.zookeeper.KeeperException
import scala.util.Sorting
import java.net.InetAddress

object SnowflakeServer {
  private val log = Logger.get
  val runtime = new RuntimeEnvironment(getClass)
  var server: TServer = null
  var workerId:Int  = -1
  val workers = new scala.collection.mutable.ListBuffer[IdWorker]()
  //TODO: what array should be passed in here?
  //val w3c = new W3CStats(Logger.get("w3c"), Array("ids_generated"))

  def shutdown(): Unit = {
    if (server != null) {
      log.info("Shutting down.")
      server.stop()
      server = null
    }
  }

  def main(args: Array[String]) {
    runtime.load(args)

    loadWorkerId()
    val admin = new AdminService(Configgy.config, runtime)

    // TODO we should sleep for at least as long as our time-drift SLA
    Thread.sleep(Configgy.config.getLong("snowflake.startup_sleep_ms", 1000L))

    try {
      val worker = new IdWorker(workerId)
      workers += worker
      val PORT = Configgy.config.getInt("snowflake.server_port", 7911)
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
        log.error(e, "Unexpected exception: %s", e.getMessage)
        throw e
      }
    }
  }

  def loadWorkerId() {
    workerId = Configgy.config.getInt("worker_id", -1)
    val zk_path = Configgy.config.getString("zookeper_worker_id_path", "/snowflake-workers")

    val watcher = new FakeWatcher;
    val zkClient = new ZookeeperClient(watcher, Configgy.config.getString("zookeeper-client.hostlist", "localhost:2181"), Configgy.config);

    while (workerId < 0) {
      try {
        zkClient.get(zk_path)
      } catch {
        case _ =>  {
          log.info("%s missing, trying to create it".format(zk_path))
          zkClient.create(zk_path, Array(), Ids.OPEN_ACL_UNSAFE, PERSISTENT)
        }
      }

      try {
        val children = zkClient.getChildren(zk_path).map((s:String) => s.toInt).toArray
        log.debug("found %s children".format(children.length))
        Sorting.quickSort(children)
        val id = findFirstAvailableId(children)

        log.debug("trying to claim workerId %d".format(id))
        zkClient.create("%s/%s".format(zk_path, id), getHostname.getBytes(), Ids.OPEN_ACL_UNSAFE, EPHEMERAL)
        log.debug("successfully claimed workerId %d".format(id))
        workerId = id;
      } catch {
        case e: KeeperException => {
          log.debug("workerId collision, retrying")
        }
      }
    }
  }

  def getHostname(): String = {
    return java.net.InetAddress.getLocalHost().getHostName();
  }

  def findFirstAvailableId(children:Array[Int]): Int = {
    if (children.length > 1) {
      for (i <- 1 until children.length) {
        if (children(i) > (children(i-1) + 1)){
          return children(i-1) + 1
        }
      }
      return children.last + 1
    } else if (children.length == 1 && children.first == 0){
      1
    } else {
      0
    }
  }
}
