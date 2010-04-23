package com.twitter.service.snowflake

import com.sun.net.httpserver.HttpExchange
import com.twitter.ostrich.{AdminHttpService, CustomHttpHandler}
import com.twitter.ostrich.Stats
import java.text.SimpleDateFormat
import java.util.Date
import net.lag.configgy.{ConfigMap, RuntimeEnvironment}


class StatusReportHandler extends CustomHttpHandler {
  def body = {<html>
    <head>
      <title>Snowflake Report</title>
    </head>
    <body>
      <table>
        <tr><td>Worker Id</td><td>{SnowflakeServer.workerId}</td></tr>
        <tr><td>Timestamp</td><td>{System.currentTimeMillis}</td></tr>
        <tr><td>Time</td><td>{new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date())}</td></tr>
        <tr><td>IDs Generated</td><td>{Stats.getCounter("ids_generated")()}</td></tr>
      </table>
    </body>
  </html>.toString}

  def handle(exchange: HttpExchange) {
    render(body, exchange, 200)
  }
}


class AdminService(config: ConfigMap, runtime: RuntimeEnvironment) {
  val adminHttp = new AdminHttpService(config, runtime)
  adminHttp.addContext("/status/", new StatusReportHandler())
  adminHttp.start()
}
