/* Copyright 2010 Twitter, Inc. */
namespace java com.twitter.service.snowflake.gen
namespace ruby Twitter.Snowflake

exception InvalidSystemClock {
  1: string message,
}

exception InvalidUserAgentError {
  1: string message,
}

service Snowflake {
  i64 get_worker_id()
  i64 get_timestamp()
  i64 get_id(1:string useragent)
  i64 get_datacenter_id()
}
