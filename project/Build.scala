import sbt._
import Keys._
import com.twitter.sbt._

object Snowflake extends Build {
  val utilVersion = "5.2.0"

  val sharedSettings = Seq(
    version := "5.1.1-SNAPSHOT",
    organization := "com.twitter",
    SubversionPublisher.subversionRepository := Some("https://svn.twitter.biz/maven-public"),
    libraryDependencies ++= Seq(
      "org.scala-tools.testing" %% "specs" % "1.6.9" % "test" withSources(),
      "junit" % "junit" % "4.8.1" % "test" withSources(),
      "org.mockito" % "mockito-all" % "1.8.5" % "test" withSources()
    ),
    resolvers += "twitter-repo" at "http://maven.twttr.com",

    ivyXML :=
      <dependencies>
        <exclude org="com.sun.jmx" module="jmxri" />
        <exclude org="com.sun.jdmk" module="jmxtools" />
        <exclude org="javax.jms" module="jms" />
      </dependencies>,

    scalacOptions ++= Seq("-encoding", "utf8"),
    scalacOptions += "-deprecation",

    // This is bad news for things like com.twitter.util.Time
    parallelExecution in Test := false,

    // This effectively disables packageDoc, which craps out
    // on generating docs for generated thrift due to the use
    // of raw java types.
    packageDoc in Compile := new java.io.File("nosuchjar"),
    
    unmanagedResourceDirectories in Compile <+= baseDirectory{ _ / "config"}
  )

  lazy val finagleCore = Project(
    id = "snowflake",
    base = file("."),
    settings = Project.defaultSettings ++
      StandardProject.newSettings ++
      CompileThrift.newSettings ++
      sharedSettings
  ).settings(
    name := "snowflake",
    libraryDependencies ++= Seq(
      "commons-codec" % "commons-codec"          % "1.4",
      "org.slf4j"     % "slf4j-api"              % "1.5.8",
      "org.slf4j"     % "slf4j-nop"              % "1.5.8",
      "thrift"        % "libthrift"              % "0.5.0",
      "com.twitter"   % "ostrich"                % "8.2.0",
      "com.twitter"   % "scala-zookeeper-client" % "3.0.6",
      "com.twitter"   % "util-logging"           % "5.3.0",
      "com.twitter"   % "util-thrift"            % "5.3.0"
    )
  )
}
