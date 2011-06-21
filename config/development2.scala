import com.twitter.service.snowflake.{SnowflakeConfig, ReporterConfig}

new SnowflakeConfig {
  val serverPort = 7610
  val datacenterId = 0
  val workerId = 1
  val adminPort = 9991
  val adminBacklog = 100
  val workerIdZkPath = "/snowflake-servers"
  val zkHostlist = "localhost"
  val skipSanityChecks = false
  val startupSleepMs = 10000
  val thriftServerThreads = 2

  val reporterConfig = new ReporterConfig {
    val scribeCategory = "snowflake"
    val scribeHost = "localhost"
    val scribePort = 1463
    val scribeSocketTimeout = 5000
    val flushQueueLimit = 100000
  }
}