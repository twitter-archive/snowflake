来自twitter的snowflake分布式id生成器。

从Scala 2.9.2升级到2.11.8，对应JDK 6升级到JDK 8。

去除service-292的依赖，因为它依赖Scala 2.9.2，只能在JDK 6下运行。
```
<parent>
    <groupId>com.twitter</groupId>
    <artifactId>service-292</artifactId>
    <version>0.0.4</version>
</parent>
```

现在Java 8已经大量使用，Java 6使用的很少了。

service-292主要用于打包成zip包，这里用maven-assembly-plugin插件代替。

thrift从0.5.0升级到0.9.3，ThsHaServer API发生变化，改成如下：
```
val tArgs = new THsHaServer.Args(transport)
tArgs.minWorkerThreads(thriftServerThreads)
tArgs.maxWorkerThreads(thriftServerThreads)
tArgs.processor(processor)

val server = new THsHaServer(tArgs)
```

去除scala-zookeeper-client依赖，它也依赖Scala 2.9.2。
由于twitter上没有更新了，将其源码(ZooKeeperClient.scala、ZookeeperClientConfig.scala)直接加入工程中。
