package com.twitter.service.snowflake

import com.twitter.ostrich.admin.RuntimeEnvironment
import com.twitter.ostrich.admin.config.ServerConfig
import com.twitter.zookeeper.ZookeeperClientConfig
import java.net.InetAddress
import com.twitter.zookeeper.ZookeeperClientConfig
import com.twitter.util.Config.RequiredValuesMissing

trait SnowflakeConfig extends ServerConfig[SnowflakeServer] {
  var serverPort = 7609
  var datacenterId = required[Int]
  var workerIdMap = required[Map[Int, String]]
  var workerIdZkPath = "/snowflake-servers"
  var skipSanityChecks = false
  var startupSleepMs = 10000
  var thriftServerThreads = 2

  var reporterConfig = required[ReporterConfig]

  var zookeeperClientConfig = required[ZookeeperClientConfig]

  def workerIdFor(host: InetAddress) = {
    workerIdMap.mapValues(
      name => name.split(':')(0)
    ).find {
      case(k,v) =>  v == host.getHostName.split(':')(0)
    }.get._1
  }

  def apply(runtime: RuntimeEnvironment) = {
    new SnowflakeServer(serverPort, datacenterId, workerIdFor(InetAddress.getLocalHost),
        workerIdZkPath, skipSanityChecks, startupSleepMs, thriftServerThreads, reporterConfig(),
        zookeeperClientConfig())
  }

  override def validate = {
    if (workerIdMap.values.size != workerIdMap.values.toSet.size)
      throw new RequiredValuesMissing("duplicate worker Ids")
    zookeeperClientConfig.validate
    reporterConfig.validate 
    super.validate
  }
}
