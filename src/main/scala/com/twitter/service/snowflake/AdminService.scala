package com.twitter.service.snowflake

import com.sun.net.httpserver.HttpExchange
import com.twitter.ostrich.admin.{AdminHttpService, CustomHttpHandler, RuntimeEnvironment}
import java.text.SimpleDateFormat
import java.util.Date
import com.twitter.ostrich.stats.Stats

class StatusHandler extends CustomHttpHandler {

  def handle(exchange: HttpExchange) {
    // val body = <html>
    //       <head>
    //     <title>Snowflake Report</title>
    //     <title>Hawkwind Cluster Health Report</title>
    //   </head>
    //   <body>
    //     <table>
    //       <tr><td>Worker Id</td><td>{SnowflakeServer.workerId}</td></tr>
    //       <tr><td>Timestamp</td><td>{System.currentTimeMillis}</td></tr>
    //       <tr><td>Time</td><td>{new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())}</td></tr>
    //       <tr><td>IDs Generated</td><td>{Stats.getCounter("ids_generated")()}</td></tr>
    //     </table>
    //   </body>
    //   </html>.toString
    // .toString
    // render(body, exchange, 200)
  }
}

class AdminService(port: Int, backlog: Int, runtime: RuntimeEnvironment) {
  val adminHttp = new AdminHttpService(port, backlog, runtime)
  adminHttp.addContext("/status/", new StatusHandler())
  adminHttp.start()
}
