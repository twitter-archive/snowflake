import sbt._
import com.twitter.sbt._
import Process._

class SnowflakeProject(info: ProjectInfo) extends StandardServiceProject(info) with CompileThriftJava with CompileThriftRuby with TartifactoryRepos {
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.5.8"
  val slf4jLog = "org.slf4j" % "slf4j-nop" % "1.5.8"
  val configgy = "net.lag" % "configgy" % "2.0.1"
  val commonsPool = "commons-pool" % "commons-pool" % "1.5.4"
  val ostrich = "com.twitter" % "ostrich" % "2.3.3"
  val sp = "org.scala-tools.testing" % "specs_2.8.0"  % "1.6.5"
  val thrift = "thrift" % "libthrift" % "0.5.0"
  val commonsCodec = "commons-codec" % "commons-codec" % "1.4"
  val zookeeperClient = "com.twitter" % "zookeeper-client" % "2.0.0"

  override def ivyXML =
    <dependencies>
      <exclude module="jms"/>
      <exclude module="jmxtools"/>
      <exclude module="jmxri"/>
      <exclude org="apache-log4j"/>
    </dependencies>

  override def pomExtra =
    <licenses>
      <license>
        <name>Apache 2</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>

  // generate a jar of just the thrift classes.
    def clientPaths = (mainCompilePath ##) / "com" / "twitter" / "service" / "snowflake" / "gen" ** "*.class"
    def packageClientAction = packageTask(clientPaths, outputPath, "snowflake-" + version.toString + "-thrift.jar", packageOptions).dependsOn(compile)
    lazy val packageClient = packageClientAction

    override def packageAction = super.packageAction.dependsOn(packageClientAction)
    override def artifacts = Set(Artifact("snowflake", "thrift"))
}
