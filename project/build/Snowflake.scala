import sbt._
import Process._


class SnowflakeProject(info: ProjectInfo) extends DefaultProject(info) {
  // Maven repositories
  val scalaToolsTesting = "testing.scala-tools.org" at "http://scala-tools.org/repo-releases/"
  val powerMock = "powermock-api" at "http://powermock.googlecode.com/svn/repo/"
  val mavenDotOrg = "repo1" at "http://repo1.maven.org/maven2/"
  val scalaToolsReleases = "scala-tools.org" at "http://scala-tools.org/repo-releases/"
  val reucon = "reucon" at "http://maven.reucon.com/public/"
  val lagDotNet = "lag.net" at "http://www.lag.net/repo/"
  val oauthDotNet = "oauth.net" at "http://oauth.googlecode.com/svn/code/maven"
  val javaDotNet = "download.java.net" at "http://download.java.net/maven/2/"
  val jBoss = "jboss-repo" at "http://repository.jboss.org/maven2/"
  val nest = "nest" at "http://www.lag.net/nest/"

  // library dependencies
  // note that JARs in lib/ are also pulled in, and so are not mentioned here
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
  val ostrich = "com.twitter" % "ostrich" % "1.1.9"
  val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.1"
  val asm = "asm" % "asm-all" % "2.2"
  val objenesis = "org.objenesis" % "objenesis" % "1.1"
  val twitter = "com.twitter" % "json" % "1.1"
  val sp = "org.scala-tools.testing" % "specs"  % "1.6.2"
  val javautils = "org.scala-tools" % "javautils" % "2.7.4-0.1"

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

  val managedSources = "src_managed" / "main"
  val managedJavaPath = managedSources / "gen-java"
  val managedResourcesPath = managedSources / "resources"
  override def mainSourceRoots = super.mainSourceRoots +++ managedJavaPath
  override def mainResources = super.mainResources +++ descendents(managedResourcesPath ##, "*")

  /**
   * Twitter specific packaging needs.
   *
   * In the classpath:
   *  - all dependencies (via Ivy/Maven and in lib)
   *  - package classes
   * On the filesystem:
   *  - scripts
   *  - config
   */
  def distPath = (
    // NOTE the double hashes (##) hoist the files in the preceeding directory
    // to the top level - putting them in the "base directory" in sbt's terminology
    ((outputPath ##) / defaultJarName) +++
    mainResources +++
    mainDependencies.scalaJars +++
    descendents(info.projectPath, "*.sh") +++
    descendents(info.projectPath, "*.awk") +++
    descendents(info.projectPath, "*.rb") +++
    descendents(info.projectPath, "*.conf") +++
    descendents(info.projectPath / "lib" ##, "*.jar") +++
    descendents(managedDependencyRootPath / "compile" ##, "*.jar")
  )

  // creates a sane classpath including all JARs and populates the manifest with it
  override def manifestClassPath = Some(
    distPath.getFiles
    .filter(_.getName.endsWith(".jar"))
    .map(_.getName).mkString(" ")
  )

  def distName = "snowflake-%s.zip".format(version)

  lazy val zip = zipTask(distPath, "dist", distName) dependsOn (`package`) describedAs("Zips up the project.")
}
