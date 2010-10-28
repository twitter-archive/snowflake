require 'rubygems'
require 'snowflake'
require "base64"
require 'trollop'

opts = Trollop::options do
  opt :e, "code to eval for each item (available as item)", :type => :string
end

d = Thrift::Deserializer.new
while l = $stdin.gets
  item = d.deserialize(AuditLogEntry.new, Base64.decode64(l.chomp))
  if opts[:e]
    eval(opts[:e])
  else
    p item
  end
end
