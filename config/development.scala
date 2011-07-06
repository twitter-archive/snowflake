import com.twitter.service.snowflake.{SnowflakeConfig, ReporterConfig}
import com.twitter.logging.config.{LoggerConfig, FileHandlerConfig}
import com.twitter.logging.Level
import com.twitter.zookeeper.ZookeeperClientConfig

new SnowflakeConfig {
  val serverPort = 7609
  val datacenterId = 0
  val workerId = 0
  val adminPort = 9990
  val adminBacklog = 100
  val workerIdZkPath = "/snowflake-servers"
  val skipSanityChecks = false
  val startupSleepMs = 10000
  val thriftServerThreads = 2

  val zookeeperClientConfig = new ZookeeperClientConfig {
    val hostList = "localhost"
  }

  val reporterConfig = new ReporterConfig {
    val scribeCategory = "snowflake"
    val scribeHost = "localhost"
    val scribePort = 1463
    val scribeSocketTimeout = 5000
    val flushQueueLimit = 100000
  }

  loggers = new LoggerConfig {
    handlers = new FileHandlerConfig {
      level = Level.TRACE
      filename = "snowflake.log"
    }
  }
}