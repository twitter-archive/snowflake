import com.twitter.service.snowflake.{SnowflakeConfig, ReporterConfig}
import com.twitter.logging.config.{LoggerConfig, FileHandlerConfig}
import com.twitter.logging.Level

new SnowflakeConfig {
  val serverPort = 7609
  val datacenterId = 0
  val workerId = 0
  val adminPort = 9990
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

  val loggerConfig = new LoggerConfig {
    handlers = new FileHandlerConfig {
      filename = "snowflake.log"
      level = Level.TRACE
    }
  }
}