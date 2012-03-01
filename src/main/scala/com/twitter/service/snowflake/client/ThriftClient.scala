/** Copyright 2010-2012 Twitter, Inc. */
package com.twitter.service.snowflake.client

import org.apache.thrift.TException
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocol}
import org.apache.thrift.transport.{TFramedTransport, TSocket, TTransport, TTransportException}
import com.twitter.service.snowflake.gen.Snowflake
import scala.reflect.Manifest
import com.twitter.logging.Logger

/**
 * T is expected to be your thrift-generated Client class. Example: Snowflake.Client
 */
class ThriftClient[T](implicit man: Manifest[T]) {
  def newClient(protocol: TProtocol)(implicit m: Manifest[T]): T = {
    val constructor = m.erasure.
    getConstructor(classOf[TProtocol])
    constructor.newInstance(protocol).asInstanceOf[T]
  }

  val log = Logger.get //FIXME
  /**
   * @param soTimeoutMS the Socket timeout for both connect and read.
   */
  def create(hostname: String, port: Int, soTimeoutMS: Int): (TTransport, T) = {
    val socket = new TSocket(hostname, port, soTimeoutMS)
    val transport = new TFramedTransport(socket)
    val protocol: TProtocol  = new TBinaryProtocol(transport)

    transport.open()
    log.debug("creating new TSocket: remote-host = %s remote-port = %d local-port = %d timeout = %d",
      hostname, socket.getSocket.getPort, socket.getSocket.getLocalPort, soTimeoutMS)
    (transport, newClient(protocol))
  }
}

