/** Copyright 2010 Twitter, Inc.*/
package com.twitter.service.snowflake

import com.twitter.service.snowflake.gen.AuditLogEntry
import java.net.Socket
import java.util.ArrayList
import org.apache.commons.codec.binary.Base64;
import org.apache.scribe.LogEntry
import org.apache.scribe.scribe.Client
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocolFactory}
import org.apache.thrift.transport.{TFramedTransport, TSocket}
import org.apache.thrift.{TBase, TException, TFieldIdEnum, TSerializer, TDeserializer}

class Reporter {

  lazy val serializer = new TSerializer(new TBinaryProtocol.Factory())
  val entries = new ArrayList[LogEntry](1)

  //cargo-culted from rockdove
  type TTBase = TBase[_ <: TFieldIdEnum]

  def report[T <: TTBase](struct: T) {
    try {
      entries.add(new LogEntry("snowflake", serialize(struct)))
      scribeClient.Log(entries)
    } catch {
      case e: Exception => println(e)
    } finally {
      entries.clear
    }
  }

  lazy val scribeClient = {
    var sock = new TSocket(new Socket("localhost", 1463))
    var transport = new TFramedTransport(sock)
    var protocol = new TBinaryProtocol(transport, false, false)
    new Client(protocol, protocol)
  }

  def serialize[T <: TTBase](struct: T): String = {
    val b64 = new Base64(0)
    b64.encodeToString(serializer.serialize(struct)) + "\n"
  }
}
