require 'thrift_client'
require 'snowflake'

count   = ARGV.shift.to_i
servers = ARGV.shift
agent   = ARGV.shift

client = ThriftClient.new(Snowflake::Client, servers, :transport_wrapper => Thrift::FramedTransport)

worker_id = client.get_worker_id

count.times do |i|
  puts [client.get_id(agent).to_s, agent, worker_id.to_s].join(' ')
end
