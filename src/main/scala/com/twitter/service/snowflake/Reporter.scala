/** Copyright 2010 Twitter, Inc.*/
package com.twitter.service.snowflake

import com.twitter.service.snowflake.gen.AuditLogEntry
import java.net.Socket
import java.util.ArrayList
import org.apache.commons.codec.binary.Base64;
import org.apache.scribe.LogEntry
import org.apache.scribe.scribe.Client
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocolFactory}
import org.apache.thrift.transport.TTransportException
import org.apache.thrift.transport.{TFramedTransport, TSocket}
import org.apache.thrift.{TBase, TException, TFieldIdEnum, TSerializer, TDeserializer}
import net.lag.logging.Logger
import java.util.concurrent.LinkedBlockingDeque
import java.net.ConnectException

class Reporter extends Runnable {
  private val log = Logger.get

  private lazy val serializer = new TSerializer(new TBinaryProtocol.Factory())

  //cargo-culted from rockdove
  type TTBase = TBase[_ <: TFieldIdEnum]

  private val queue = new LinkedBlockingDeque[TTBase]
  private var scribeClient : Option[Client] = None
  private val structs = new ArrayList[TTBase](100)
  private val entries = new ArrayList[LogEntry](100)
  val thread = new Thread(this)
  thread.start

  def report[T <: TTBase](struct: T) {
    try {
      queue.put(struct)
    } catch {
      case e => {
      }
    }
  }

  override def run {
    while(true) {
      connect
      queue.drainTo(structs, 100)
      if (structs.size > 0) {
        for (i <- 0 until structs.size) {
          entries.add(i, new LogEntry("snowflake", serialize(structs.get(i))))
        }
        try {
          scribeClient.get.Log(entries) //TODO timeout
          log.debug("reported %d items to scribe. queue is %d".format(entries.size, queue.size))
        } catch {
          case e: TTransportException =>  { handle_exception(e, structs) }
          case e: ConnectException    =>  { handle_exception(e, structs) }
        } finally {
          structs.clear
          entries.clear
        }
      } else {
        log.debug("no items. gonna back off")
        Thread.sleep(100)
      }
    }
  }

  private def handle_exception[T <: TTBase](e: Throwable, items: ArrayList[T]) {
    scribeClient = None

    log.error("caught a thrift error. gonna chill for a bit. queue is %d".format(queue.size))
    Thread.sleep(1000)
  }

  private def connect {
    while(scribeClient.isEmpty) {
      try {
        var sock = new TSocket(new Socket("localhost", 1463))
        var transport = new TFramedTransport(sock)
        var protocol = new TBinaryProtocol(transport, false, false)
        scribeClient = Some(new Client(protocol, protocol))
      } catch {
        case e: ConnectException => {
          log.debug("failed to created scribe client"); 
          Thread.sleep(1000)
        }
      }
    } 
  }

  private def serialize[T <: TTBase](struct: T): String = {
    val b64 = new Base64(0)
    b64.encodeToString(serializer.serialize(struct)) + "\n"
  }

  private def logError(e: Throwable) {
    log.error(e, "Unexpected exception while scribing: %s", e.getMessage)
    log.error(e.getClass.getName + "\n" +
      e.getStackTrace.map { st =>
        import st._
        "  "+getClassName+"."+getMethodName +
        "("+getFileName+":"+getLineNumber+")"
      }.mkString("\n")
    )
  }
}
