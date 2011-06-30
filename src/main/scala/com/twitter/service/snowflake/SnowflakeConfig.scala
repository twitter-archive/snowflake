package com.twitter.service.snowflake

import com.twitter.ostrich.admin.RuntimeEnvironment
import com.twitter.ostrich.admin.config.ServerConfig

trait SnowflakeConfig extends ServerConfig[SnowflakeServer] {
  val serverPort: Int
  val datacenterId: Int
  val workerId: Int
  val adminPort: Int
  val adminBacklog: Int
  val workerIdZkPath: String
  val zkHostlist: String
  val skipSanityChecks: Boolean
  val startupSleepMs: Int
  val thriftServerThreads: Int

  val reporterConfig: ReporterConfig

  def apply(runtime: RuntimeEnvironment) = {
    new SnowflakeServer(this)
  }
}