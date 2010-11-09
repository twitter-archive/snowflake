package com.twitter.service.snowflake

import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import org.specs._

class IdWorkerSpec extends Specification {
  Configgy.configure("config/test.conf")
  val workerMask     = 0x000000000001F000L
  val datacenterMask = 0x00000000003E0000L
  val timestampMask  = 0xFFFFFFFFFFC00000L

  class EasyTimeWorker(workerId: Long, datacenterId: Long) extends IdWorker(workerId, datacenterId) {
    var timeMaker = (() => System.currentTimeMillis)
    override def timeGen(): Long = {
      timeMaker()
    }
  }

  class WakingIdWorker(workerId: Long, datacenterId: Long) extends EasyTimeWorker(workerId, datacenterId) {
    var slept = 0
    override def tilNextMillis(lastTimestamp:Long): Long = {
      slept += 1
      super.tilNextMillis(lastTimestamp)
    }
  }

  "IdWorker" should {

    "generate an id" in {
      val s = new IdWorker(1, 1)
      val id: Long = s.nextId()
      id must be_>(0L)
    }

    "return an accurate timestamp" in {
      val s = new IdWorker(1, 1)
      val t = System.currentTimeMillis
      (s.get_timestamp() - t) must be_<(50L)
    }

    "return the correct job id" in {
      val s = new IdWorker(1, 1)
      s.get_worker_id() must be_==(1L)
    }

    "return the correct dc id" in {
      val s = new IdWorker(1, 1)
      s.get_datacenter_id() must be_==(1L)
    }

    "properly mask worker id" in {
      val workerId = 0x1F
      val datacenterId = 0
      val worker = new IdWorker(workerId, datacenterId)
      for (i <- 1 to 1000) {
        val id = worker.nextId
        ((id & workerMask) >> 12) must be_==(workerId)
      }
    }

    "properly mask dc id" in {
      val workerId = 0
      val datacenterId = 0x1F
      val worker = new IdWorker(workerId, datacenterId)
      val id = worker.nextId
      ((id & datacenterMask) >> 17) must be_==(datacenterId)
    }

    "properly mask timestamp" in {
      val worker = new EasyTimeWorker(31, 31)
      for (i <- 1 to 100) {
        val t = System.currentTimeMillis
        worker.timeMaker = (() => t)
        val id = worker.nextId
        ((id & timestampMask) >> 22)  must be_==(t - worker.twepoch)
      }
    }

    "roll over sequence id" in {
      // put a zero in the low bit so we can detect overflow from the sequence
      val workerId = 4
      val datacenterId = 4
      val worker = new IdWorker(workerId, datacenterId)
      val startSequence = 0xFFFFFF-20
      val endSequence = 0xFFFFFF+20
      worker.sequence = startSequence

      for (i <- startSequence to endSequence) {
        val id = worker.nextId
        ((id & workerMask) >> 12) must be_==(workerId)
      }
    }

    "generate increasing ids" in {
      val worker = new IdWorker(1, 1)
      var lastId = 0L
      for (i <- 1 to 100) {
        val id = worker.nextId
        id must be_>(lastId)
        lastId = id
      }
    }

    "generate 1 million ids quickly" in {
      val worker = new IdWorker(31, 31)
      val t = System.currentTimeMillis
      for (i <- 1 to 1000000) {
        var id = worker.nextId
        id
      }
      val t2 = System.currentTimeMillis
      println("generated 1000000 ids in %d ms, or %,.0f ids/second".format(t2 - t, 1000000000.0/(t2-t)))
      1 must be_>(0)
    }

    "sleep if we would rollover twice in the same millisecond" in {
      var queue = new scala.collection.mutable.Queue[Long]()
      val worker = new WakingIdWorker(1, 1)
      val iter = List(2L, 2L, 3L).elements
      worker.timeMaker = (() => iter.next)
      worker.sequence = 4095
      worker.nextId
      worker.sequence = 4095
      worker.nextId
      worker.slept must be(1)
    }

    "generate only unique ids" in {
      val worker = new IdWorker(31, 31)
      var set = new scala.collection.mutable.HashSet[Long]()
      val n = 2000000
      (1 to n).foreach{i =>
        val id = worker.nextId
        if (set.contains(id)) {
          println(java.lang.Long.toString(id, 2))
        } else {
          set += id
        }
      }
      set.size must be_==(n)
    }

    "generate ids over 50 billion" in {
      val worker = new IdWorker(0, 0)
      worker.nextId must be_>(50000000000L)
    }
  }

  "validUseragent" should {
    "accept the simple case" in {
      val worker = new IdWorker(1, 1)
      worker.validUseragent("infra-dm") must be_==(true);
    }

    "reject leading numbers" in {
      val worker = new IdWorker(1, 1)
      worker.validUseragent("1") must be_==(false)
      worker.validUseragent("1asdf") must be_==(false)
    }
  }
}
