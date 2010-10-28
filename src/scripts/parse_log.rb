require 'rubygems'
require 'snowflake'
require "base64"

f = File.new("/tmp/scribe/snowflake_current", "r")

d = Thrift::Deserializer.new

while l = f.gets
  p d.deserialize(AuditLogEntry.new, Base64.decode64(l.chomp))
end
