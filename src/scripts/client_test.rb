#!/usr/bin/ruby 
require 'rubygems'
require 'snowflake'

if ARGV.length < 3
  puts "client_test.rb <count> <servers> <agent>"
  exit
end
count   = ARGV.shift.to_i
servers = ARGV.shift
agent   = ARGV.shift

host, port = servers.split(/,/).first.split(/:/)
p host
p port
socket = Thrift::Socket.new(host, port.to_i, 1)
socket.open
connection = Thrift::FramedTransport.new(socket)
client = Snowflake::Client.new(Thrift::BinaryProtocol.new(connection))

worker_id = client.get_worker_id

count.times do |i|
  puts [client.get_id(agent).to_s, agent, worker_id.to_s].join(' ')
end
