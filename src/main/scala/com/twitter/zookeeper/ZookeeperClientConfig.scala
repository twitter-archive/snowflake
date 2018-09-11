package com.twitter.zookeeper

import com.twitter.util.Config

trait ZookeeperClientConfig extends Config[ZooKeeperClient] {
  var hostList = required[String]
  var sessionTimeout = 3000
  var basePath = ""

  def apply = {
    new ZooKeeperClient(this)
  }
}
