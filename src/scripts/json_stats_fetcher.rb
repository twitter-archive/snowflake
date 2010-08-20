#!/usr/bin/ruby

require 'rubygems'
require 'getoptlong'
require 'socket'
require 'json'
require 'open-uri'

def report_metric(name, value, units)
  if $report_to_ganglia
    system("gmetric -t float -n \"#{$ganglia_prefix}#{name}\" -v \"#{value}\" -u \"#{units}\" -d #{$stat_timeout}")
  else
    puts "#{$ganglia_prefix}#{name}=#{value} #{units}"
  end
end

$report_to_ganglia = true
$ganglia_prefix = ''
$stat_timeout = 86400

hostname = "localhost"
port = 7610
use_web = false

def usage(port)
  puts
  puts "usage: json_stats_fetcher.rb [options]"
  puts "options:"
  puts "    -n              say what I would report, but don't report it"
  puts "    -w              use web interface"
  puts "    -h <hostname>   connect to another host (default: localhost)"
  puts "    -p <port>       connect to another port (default: #{port})"
  puts "    -P <prefix>     optional prefix for ganglia names"
  puts
end

opts = GetoptLong.new(
  [ '--help', GetoptLong::NO_ARGUMENT ],
  [ '-n', GetoptLong::NO_ARGUMENT ],
  [ '-h', GetoptLong::REQUIRED_ARGUMENT ],
  [ '-p', GetoptLong::REQUIRED_ARGUMENT ],
  [ '-P', GetoptLong::REQUIRED_ARGUMENT ],
  [ '-w', GetoptLong::NO_ARGUMENT ]
  )

opts.each do |opt, arg|
  case opt
  when '--help'
    usage(port)
    exit 0
  when '-n'
    $report_to_ganglia = false
  when '-h'
    hostname = arg
  when '-p'
    port = arg.to_i
  when '-P'
    $ganglia_prefix = arg
  when '-w'
    port = 7610 # TODO: read from config file
    use_web = true
  end
end

stats_dir = "/tmp/stats-#{port}"
singleton_file = "#{stats_dir}/json_stats_fetcher_running"

Dir.mkdir(stats_dir) rescue nil

if File.exist?(singleton_file)
  puts "NOT RUNNING -- #{singleton_file} exists."
  puts "Kill other stranded stats checker processes and kill this file to resume."
  exit 1
end
File.open(singleton_file, "w") { |f| f.write("i am running.\n") }

begin
  data = if use_web
    open("http://#{hostname}:#{port}/stats#{'?reset=1' if $report_to_ganglia}").read
  else
    socket = TCPSocket.new(hostname, port)
    socket.puts("stats/json#{' reset' if $report_to_ganglia}")
    socket.gets
  end

  stats = JSON.parse(data)

  report_metric("jvm_threads", stats["jvm"]["thread_count"], "threads")
  report_metric("jvm_daemon_threads", stats["jvm"]["thread_daemon_count"], "threads")
  report_metric("jvm_heap_used", stats["jvm"]["heap_used"], "bytes")
  report_metric("jvm_heap_max", stats["jvm"]["heap_max"], "bytes")

  stats["counters"].each do |name, value|
    report_metric(name, (value.to_i rescue 0), "items")
  end

  stats["gauges"].each do |name, value|
    report_metric(name, value, "value")
  end

  stats["timings"].each do |name, timing|
    report_metric(name, (timing["average"] || 0).to_f / 1000.0, "sec")
    report_metric("#{name}_stddev", (timing["standard_deviation"] || 0).to_f / 1000.0, "sec")
    [:hist_25, :hist_50, :hist_75, :hist_99, :hist_999, :hist_9999].map(&:to_s).each do |bucket|
      report_metric("#{name}_#{bucket}", (timing[bucket] || 0).to_f / 1000.0, "sec") if timing[bucket]
    end
  end
ensure
  File.unlink(singleton_file)
end
