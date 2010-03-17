/** Copyright 2008 Twitter, Inc. */
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


object SnowflakeServer {
  private val log = Logger.get
  val runtime = new RuntimeEnvironment(getClass)
  var server: TServer = null
  var serverId:Int = 0
  val workers = new scala.collection.mutable.ListBuffer[Snowflake]()
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

    serverId = Configgy.config.getInt("server_id").get
    val admin = new AdminService(Configgy.config, runtime)

    try {
      // paranoia to make sure we don't restart too quickly
      // and cause ID collisions
      Thread.sleep(1000)
    }

    try {
      val worker = new Snowflake(serverId)
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
}
