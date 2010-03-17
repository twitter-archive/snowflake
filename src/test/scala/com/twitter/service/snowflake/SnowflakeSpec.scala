package com.twitter.service.snowflake

import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import org.specs._

class SnowflakeSpec extends Specification {
  Configgy.configure("config/test.conf")

  "Snowflake" should {
    "generate an id" in {
      val s = new Snowflake(1)
      val id: Long = s.get_id()
      id must be_>(0L)
    }

    "return an accurate timestamp" in {
      val s = new Snowflake(1)
      val t = System.currentTimeMillis
      (s.get_timestamp() - t) must be_<(50L)
    }

    "return the correct job id" in {
      val s = new Snowflake(1)
      s.get_worker_id() must be_==(1L)
    }
  }
}
