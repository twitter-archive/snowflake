package com.twitter.service.snowflake

import com.twitter.util.Config

trait ReporterConfig extends Config[Reporter] {
  var scribeCategory = "snowflake"
  var scribeHost = "localhost"
  var scribePort =  1463
  var scribeSocketTimeout =  5000
  var flushQueueLimit = 100000

  def apply = {
    new Reporter(scribeCategory, scribeHost, scribePort, scribeSocketTimeout, flushQueueLimit)
  }
}
