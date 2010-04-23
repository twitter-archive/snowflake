package com.twitter.service.snowflake

import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import org.specs._

class SnowflakeServerSpec extends Specification {
  Configgy.configure("config/test.conf")

  "findFirstAvailableId" should {
    "return 0 for empty Array" in {
      SnowflakeServer.findFirstAvailableId(Array()) must be_==(0)
    }

    "return 1 for Array(0)" in {
      SnowflakeServer.findFirstAvailableId(Array(0)) must be_==(1)
    }

    "return 1 for Array(0,2)" in {
      SnowflakeServer.findFirstAvailableId(Array(0,2)) must be_==(1)
    }
    
    "return 4 for Array(0,1,2,3)" in {
      SnowflakeServer.findFirstAvailableId(Array(0,1,2,3)) must be_==(4)
    }

    "return 0 if its available" in {
      SnowflakeServer.findFirstAvailableId(Array(1)) must be_==(0)
    }
  }

}
