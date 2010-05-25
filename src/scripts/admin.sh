#!/bin/sh
exec scala -classpath @TEST_CLASSPATH@ $0 $@
!#

import com.facebook.thrift.protocol.TBinaryProtocol
import com.facebook.thrift.transport.TSocket
import com.twitter.service.Admin

val socket = new TSocket("localhost", 9991)
socket.open
val client = new Admin.Client(new TBinaryProtocol(socket))
val stats = client.stats
println(stats)
