#!/usr/bin/ruby
require 'rubygems'
require 'thrift_client'
require 'snowflake'

if ARGV.length < 2
  puts "connections_test.rb <count> <servers>"
  exit
end
count   = ARGV.shift.to_i
servers = ARGV.shift

count.times do |i|
  client = ThriftClient.new(Snowflake::Client, servers.split(/,/), :transport_wrapper => Thrift::FramedTransport, :randomize_server_list => true)
  client.get_id('foo')
end
