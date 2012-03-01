/** Copyright 2010-2012 Twitter, Inc.*/
package com.twitter.service.snowflake

import com.twitter.ostrich.admin.BackgroundProcess
import com.twitter.ostrich.stats.Stats
import com.twitter.service.snowflake.gen.AuditLogEntry
import java.io.ByteArrayOutputStream
import java.net.ConnectException
import java.net.Socket
import java.util.ArrayList
import java.util.concurrent.LinkedBlockingDeque
import org.apache.commons.codec.binary.Base64;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.scribe.LogEntry
import org.apache.scribe.scribe.Client
import org.apache.thrift.protocol.{TBinaryProtocol, TProtocolFactory}
import org.apache.thrift.transport.{TTransportException, TFramedTransport, TSocket}
import org.apache.thrift.{TBase, TException, TFieldIdEnum, TSerializer, TDeserializer}
import com.twitter.logging.Logger

class Reporter(scribeCategory: String, scribeHost: String, scribePort: Int, 
  scribeSocketTimeout: Int, flushQueueLimit: Int) {
  private val log = Logger.get
  val queue = new LinkedBlockingDeque[TBase[_,_]](flushQueueLimit)
  private val structs = new ArrayList[TBase[_,_]](100)
  private val entries = new ArrayList[LogEntry](100)
  private var scribeClient: Option[Client] = None
  private val baos = new ByteArrayOutputStream
  private val protocol = (new TBinaryProtocol.Factory).getProtocol(new TIOStreamTransport(baos))

  Stats.addGauge("reporter_flush_queue") { queue.size() }
  val enqueueFailuresCounter = Stats.getCounter("scribe_enqueue_failures")
  val exceptionCounter = Stats.getCounter("exceptions")

  val thread = new BackgroundProcess("Reporter flusher") {
    def runLoop {
      try {
        connect
        queue.drainTo(structs, 100)
        if (structs.size > 0) {
          for (i <- 0 until structs.size) {
            entries.add(i, new LogEntry(scribeCategory, serialize(structs.get(i))))
          }
          scribeClient.get.Log(entries)
          log.trace("reported %d items to scribe. queue is %d".format(entries.size, queue.size))
        } else {
          log.trace("no items. gonna back off")
          Thread.sleep(1000)
        }
      } catch {
        case e: TTransportException =>  { handle_exception(e, structs) }
      } finally {
        structs.clear
        entries.clear
      }
    }

    private def handle_exception(e: Throwable, items: ArrayList[TBase[_,_]]) {
      exceptionCounter.incr(1)
      for(i <- items.size until 0) {
        val success = queue.offerFirst(items.get(i))
        if (!success) {
          log.error("unable to reenqueue item on failure")
        }
      }

      scribeClient = None
      log.error("caught a thrift error. gonna chill for a bit. queue is %d".format(queue.size))
      logError(e)
      Thread.sleep(10000)
    }

    private def connect {
      while(scribeClient.isEmpty) {
        try {
          log.debug("connection to scribe at %s:%d with timeout %d".format(scribeHost, scribePort, scribeSocketTimeout))
          var sock = new TSocket(scribeHost, scribePort, scribeSocketTimeout)
          sock.open()
          var transport = new TFramedTransport(sock)
          var protocol = new TBinaryProtocol(transport, false, false)
          scribeClient = Some(new Client(protocol, protocol))
        } catch {
          case e: TTransportException => {
            exceptionCounter.incr(1)
            log.debug("failed to created scribe client")
            Thread.sleep(10000)
          }
        }
      }
    }

    private def serialize(struct: TBase[_,_]): String = {
      val b64 = new Base64(0)
      baos.reset
      struct.write(protocol)
      b64.encodeToString(baos.toByteArray) + "\n"
    }
  }
  thread.start

  def report(struct: TBase[_,_]) {
    try {
      val success = queue.offer(struct)
      if (!success) {
        log.error("Failed to enueue entry for scribe queue. Queue size is %d".format(queue.size))
        enqueueFailuresCounter.incr()
      }
    } catch {
      case e => {
        exceptionCounter.incr(1)
        logError(e)
      }
    }
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

  override def finalize() {
    thread.shutdown
  }
}
