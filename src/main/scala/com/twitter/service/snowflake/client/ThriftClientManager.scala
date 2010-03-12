/** Copyright 2009 Twitter, Inc. */
package com.twitter.service.snowflake.client

import com.twitter.service.snowflake.gen.Snowflake.Client
import com.facebook.thrift.TException
import com.facebook.thrift.protocol.{TBinaryProtocol, TProtocol}
import com.facebook.thrift.transport.{TFramedTransport, TSocket, TTransport, TTransportException}
import net.lag.configgy.Configgy
import net.lag.logging.Logger


/**
 * Manages access to the Thrift RPC client.
 */
object ThriftClientManager {
  private val log = Logger.get

  private val soTimeoutMS = Configgy.config.getInt("thrift.so-timeout-ms", 100)


  /**
   * Returns a Tuple of (TTransport, Client). The TTransport returned has already been opened once
   * but you should still check to make sure it's valid and doesn't need to be re-opened.
   */
  def apply[T](hostname: String, port: Int): (TTransport, Client) = {
    val socket = new TSocket(hostname, port, soTimeoutMS)
    val transport = new TFramedTransport(socket)
    val protocol: TProtocol  = new TBinaryProtocol(transport)
    transport.open()
    log.debug("creating new TSocket: remote-host = %s remote-port = %d local-port = %d timeout = %d",
      hostname, socket.getSocket.getPort, socket.getSocket.getLocalPort, soTimeoutMS)
    (transport, new Client(protocol))
  }
}
