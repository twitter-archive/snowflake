/** Copyright 2010-2011 Twitter, Inc. */
package com.twitter.service.snowflake.client
import com.twitter.service.snowflake.gen.Snowflake

import net.lag.configgy.ConfigMap


object SnowflakeClient extends ThriftClient[Snowflake.Client]

