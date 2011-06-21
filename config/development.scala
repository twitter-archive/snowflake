import com.twitter.service.snowflake.{SnowflakeConfig, ReporterConfig}

new SnowflakeConfig {
  val serverPort: Int = 7609
  val datacenterId: Int = 0
  val workerId: Int = 0
  val adminPort: Int = 9990
  val adminBacklog: Int = 100
  val workerIdZkPath: String = "/snowflake-servers"
  val zkHostlist: String = "localhost"
  val skipSanityChecks: Boolean = false
  val startupSleepMs: Int = 10000
  val thriftServerThreads: Int = 2

  val reporterConfig = new ReporterConfig {
    val scribeCategory = "snowflake"
    val scribeHost = "localhost"
    val scribePort = 1463
    val scribeSocketTimeout = 5000
    val flushQueueLimit = 100000
  }
}