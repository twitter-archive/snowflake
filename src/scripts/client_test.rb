#!/usr/bin/ruby 
require 'rubygems'
require 'thrift_client'
require 'snowflake'

if ARGV.length < 3
  puts "client_test.rb <count> <servers> <agent>"
  exit
end
count   = ARGV.shift.to_i
servers = ARGV.shift
agent   = ARGV.shift

client = ThriftClient.new(Snowflake::Client, servers.split(/,/), :transport_wrapper => Thrift::FramedTransport, :randomize_server_list => true)

worker_id = client.get_worker_id

count.times do |i|
  puts [client.get_id(agent).to_s, agent, worker_id.to_s].join(' ')
end
