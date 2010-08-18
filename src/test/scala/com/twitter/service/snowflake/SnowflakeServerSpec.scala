package com.twitter.service.snowflake

import net.lag.configgy.{Config, Configgy, RuntimeEnvironment}
import org.specs._

class SnowflakeServerSpec extends Specification {
  Configgy.configure("config/test.conf")

}
