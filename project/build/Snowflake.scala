import sbt._
import com.twitter.sbt._
import Process._


class SnowflakeProject(info: ProjectInfo) extends StandardProject(info) {
  // Maven repositories
  val mavenDotOrg = "repo1" at "http://repo1.maven.org/maven2/"
  val lagDotNet = "lag.net" at "http://www.lag.net/repo/"
  val jBoss = "jboss-repo" at "http://repository.jboss.org/maven2/"
  val nest = "nest" at "http://www.lag.net/nest/"

  // library dependencies
  // note that JARs in libs/ are also pulled in, and so are not mentioned here
  val vscaladoc = "org.scala-tools" % "vscaladoc" % "1.1-md-3"
  val markdownj = "markdownj" % "markdownj" % "1.0.2b4-0.3.0"
  val slf4jApi = "org.slf4j" % "slf4j-api" % "1.5.8"
  val slf4jLog = "org.slf4j" % "slf4j-log4j12" % "1.5.8"
  val log4j = "apache-log4j" % "log4j" % "1.2.15"
  val commonsLogging = "commons-logging" % "commons-logging" % "1.1"
  val commonsLang = "commons-lang" % "commons-lang" % "2.2"
  val oro = "oro" % "oro" % "2.0.7"
  val configgy = "net.lag" % "configgy" % "1.4.7"
  val mockito = "org.mockito" % "mockito-core" % "1.8.1"
  val xrayspecs = "com.twitter" % "xrayspecs" % "1.0.5"
  val commonsPool = "commons-pool" % "commons-pool" % "1.5.4"
  val commonsCollections = "commons-collections" % "commons-collections" % "3.2.1"
  val ostrich = "com.twitter" % "ostrich" % "1.1.15"
  val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.1"
  val asm = "asm" % "asm-all" % "2.2"
  val objenesis = "org.objenesis" % "objenesis" % "1.1"
  val json = "com.twitter" % "json" % "1.1"
  val sp = "org.scala-tools.testing" % "specs"  % "1.6.2.2"
  val javautils = "org.scala-tools" % "javautils" % "2.7.4-0.1"
  val thrift = "thrift" % "libthrift" % "0.2.0"
  val zookeeperClient = "com.twitter" % "zookeeper-client" % "1.1"

  def generatedThriftDirectoryPath = "src_managed" / "main"
  def thriftDirectoryPath = "src" / "main" / "thrift"
  def thriftFile = thriftDirectoryPath / "Snowflake.thrift"

  def thriftTask(lang: String, directory: Path, thriftFile: Path) = {
    val cleanIt = cleanTask(directory / ("gen-" + lang)) named("clean-thrift-" + lang)
    val createIt = task { FileUtilities.createDirectory(directory, log) } named("create-managed-src-dir-" + lang)

    execTask {
      <x>thrift --gen {lang} -o {directory.absolutePath} {thriftFile.absolutePath}</x>
    } dependsOn(cleanIt, createIt)
  }

  lazy val thriftJava = thriftTask("java", generatedThriftDirectoryPath, thriftFile) describedAs("Build Thrift Java")

  override def disableCrossPaths = true
  override def compileAction = super.compileAction dependsOn(thriftJava)
  override def compileOrder = CompileOrder.JavaThenScala
  override def mainClass = Some("com.twitter.service.snowflake.SnowflakeServer")
  override def releaseBuild = true
}
