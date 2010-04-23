/* Copyright 2010 Twitter, Inc. */
namespace java com.twitter.service.snowflake.gen
namespace ruby Twitter.Snowflake

/**
 * This is what the Merge Layer talks to.
 */

service Snowflake {
  i64 get_worker_id()
  i64 get_timestamp()
  i64 get_id()
}
