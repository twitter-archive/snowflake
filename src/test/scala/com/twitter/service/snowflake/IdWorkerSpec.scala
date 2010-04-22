package com.twitter.service.snowflake

import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import org.specs._

class IdWorkerSpec extends Specification {
  Configgy.configure("config/test.conf")
  val workerMask =    0x00000000003FF000L
  val timestampMask = 0xFFFFFFFFFFC00000L

  class WakingIdWorker(workerId: Long) extends IdWorker(workerId) {
    var slept = 0
    override def tilNextMillis(lastTimestamp:Long, timeGen:(() => Long)): Long = {
      slept += 1
      super.tilNextMillis(lastTimestamp, timeGen)
    }
   }
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
        val id = worker.nextId(() => t)
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
      for (i <- 1 to 3000000) {
        var id = worker.nextId
        id
      }
      val t2 = System.currentTimeMillis
      println("generated 3000000 ids in %d ms, or %,.0f ids/second".format(t2 - t, 1000000000.0/(t2-t)))
      1 must be_>(0)
    }

    "sleep if we would rollover twice in the same millisecond" in {
      var queue = new scala.collection.mutable.Queue[Long]()
      val worker = new WakingIdWorker(1)
      val iter = List(2L, 2L, 3L).elements
      val msGen = (() => iter.next)
      worker.sequence = 4095
      worker.nextId(msGen)
      worker.sequence = 4095
      worker.nextId(msGen)
      worker.slept must be(1)
    }

    "generate only unique ids" in {
      val worker = new IdWorker(255)
      var set = new scala.collection.mutable.HashSet[Long]()
      val ids = (1 to 2000000).map(i => worker.nextId).force
      ids.foreach{id =>
        if (set.contains(id)) {
          println(java.lang.Long.toString(id, 2))
        } else {
          set += id
        }
      }
      set.size must be_==(ids.size)
    }
  }
}
