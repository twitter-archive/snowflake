import sbt._
import com.twitter.sbt._
import Process._


class SnowflakeProject(info: ProjectInfo) extends StandardProject(info) {
  // Maven repositories
  val mavenDotOrg = "repo1" at "http://repo1.maven.org/maven2/"
  val jBoss = "jboss-repo" at "http://repository.jboss.org/maven2/"

  // library dependencies
  // note that JARs in libs/ are also pulled in, and so are not mentioned here
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.5.8"
  val slf4jLog = "org.slf4j" % "slf4j-log4j12" % "1.5.8"
  val log4j = "apache-log4j" % "log4j" % "1.2.15"
  val configgy = "net.lag" % "configgy" % "1.6.8"
  val commonsPool = "commons-pool" % "commons-pool" % "1.5.4"
  val ostrich = "com.twitter" % "ostrich" % "1.1.24"
  val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.1"
  val sp = "org.scala-tools.testing" % "specs"  % "1.6.2.2"
  val thrift = "thrift" % "libthrift" % "0.5.0"
  val commonsCodec = "commons-codec" % "commons-codec" % "1.4"
  val zookeeperClient = "com.twitter" % "zookeeper-client" % "1.5.1"

  override def mainClass = Some("com.twitter.service.snowflake.SnowflakeServer")
  override def releaseBuild = true

  override def pomExtra =
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
}
