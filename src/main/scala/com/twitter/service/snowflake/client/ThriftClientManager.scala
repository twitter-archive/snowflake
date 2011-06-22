/** Copyright 2010 Twitter, Inc. */
package com.twitter.service.snowflake.client

import com.twitter.service.snowflake.gen.Snowflake.Client
import org.apache.thrift.TException
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocol}
import org.apache.thrift.transport.{TFramedTransport, TSocket, TTransport, TTransportException}
import com.twitter.logging.Logger

/**
 * Manages access to the Thrift RPC client.
 */
object ThriftClientManager {
  private val log = Logger.get //FIXME

  /**
   * Returns a Tuple of (TTransport, Client). The TTransport returned has already been opened once
   * but you should still check to make sure it's valid and doesn't need to be re-opened.
   */
  def apply[T](hostname: String, port: Int): (TTransport, Client) = {
    val socket = new TSocket(hostname, port, 1000)
    val transport = new TFramedTransport(socket)
    val protocol: TProtocol  = new TBinaryProtocol(transport)
    transport.open()
    log.debug("creating new TSocket: remote-host = %s remote-port = %d local-port = %d timeout = %d",
      hostname, socket.getSocket.getPort, socket.getSocket.getLocalPort, 10000)
    (transport, new Client(protocol))
  }
}
