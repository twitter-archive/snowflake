package com.twitter.service.snowflake

import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import org.specs._

class IdWorkerSpec extends Specification {
  Configgy.configure("config/test.conf")
  val workerMask =    0x00000000003FF000L
  val timestampMask = 0xFFFFFFFFFFC00000L

  "IdWorker" should {
    "properly mask server id" in {
      val workerId = 0xFF
      val worker = new IdWorker(workerId)
      for (i <- 1 to 1000) {
        val id = worker.nextId
        ((id & workerMask) >> 12) must be_==(workerId)
      }
    }

    "properly mask timestamp" in {
      val worker = new IdWorker(255)
      for (i <- 1 to 100) {
        val t = System.currentTimeMillis
        val id = worker.nextId(t)
        ((id & timestampMask) >> 22)  must be_==(t - worker.twepoch)
      }
    }

    "roll over sequence id" in {
      // put a zero in the low bit so we can detect overflow from the sequence
      val workerId = 4
      val worker = new IdWorker(workerId)
      val startSequence = 0xFFFFFF-20
      val endSequence = 0xFFFFFF+20
      worker.sequence = startSequence

      for (i <- startSequence to endSequence) {
        val id = worker.nextId
        ((id & workerMask) >> 12) must be_==(workerId)
      }
    }

    "generate increasing ids" in {
      val worker = new IdWorker(1)
      var lastId = 0L
      for (i <- 1 to 100) {
        val id = worker.nextId
        id must be_>(lastId)
        lastId = id
      }
    }

    "generate 1 million ids quickly" in {
      val worker = new IdWorker(255)
      val t = System.currentTimeMillis
      for (i <- 1 to 1000000) {
        var id = worker.nextId
        id
      }
      val t2 = System.currentTimeMillis
      println("generated 1000000 ids in %d ms, or %,.0f ids/second".format(t2 - t, 1000000000.0/(t2-t)))
      1 must be_>(0)
    }
  }
}
