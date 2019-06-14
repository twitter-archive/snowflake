/* Copyright 2010-2012 Twitter, Inc. */
namespace java com.twitter.service.snowflake.gen

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

struct AuditLogEntry {
  1: i64 id,
  2: string useragent,
  3: i64 tag
}