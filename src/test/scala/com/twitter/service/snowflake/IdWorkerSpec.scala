package com.twitter.service.snowflake

import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import org.specs._

object SnowflakeSpec extends Specification {
  Configgy.configure("config/test.conf")

  "Snowflake" should {
    "properly mask server id" in {
      val workerId = 0xFF
      val worker = new IdWorker(workerId)
      val workerMask = 0x00000000FF000000L
      for (i <- 1 to 1000) {
        val id = worker.nextId
        ((id & workerMask) >> 24) must be_==(workerId)
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
