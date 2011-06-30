package com.twitter.service.snowflake

trait ReporterConfig {
  val scribeCategory: String
  val scribeHost: String
  val scribePort: Int
  val scribeSocketTimeout: Int
  val flushQueueLimit: Int
}