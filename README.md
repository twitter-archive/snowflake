We have retired the initial release of Snowflake and working on open sourcing
the next version based on
[Twitter-server](https://twitter.github.io/twitter-server/), in a form that can
run anywhere without requiring Twitter's own infrastructure services.

The initial version, released in 2010, was based on Apache Thrift and it
predated [Finagle](https://twitter.github.io/twitter-server/), our building
block for RPC services at Twitter.  The Snowflake we're using internally is a
full rewrite and heavily relies on existing infrastructure at Twitter to run.
We cannot commit to a date but we're doing our best to add necessary features to
make Snowflake fit for many environments outside of Twitter.

Source code is still in the repository and is reachable from
[snowflake-2010](https://github.com/twitter/snowflake/releases/tag/snowflake-2010)
tag.

We won't be accepting pull requests or responding to issues for the retired
release.
