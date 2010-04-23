/* Copyright 2008 Twitter, Inc. */
namespace java com.twitter.service.snowflake.gen
namespace ruby Twitter.Snowflake

service Snowflake {
  i64 get_worker_id()
  i64 get_timestamp()
  i64 get_id(1:string useragent)
}
