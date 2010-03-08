package com.twitter.service.snowflake

import com.sun.net.httpserver.HttpExchange
import com.twitter.ostrich.{AdminHttpService, CustomHttpHandler}
import com.twitter.ostrich.Stats
import net.lag.configgy.{ConfigMap, RuntimeEnvironment}


class StatusReportHandler extends CustomHttpHandler {
  val body = <html>
    <head>
      <title>Snowflake Report</title>
    </head>
    <body>
      <h1>IDs Generated</h1>
      { Stats.getCounter("ids_generated")() }
    </body>
  </html>.toString

  def handle(exchange: HttpExchange) {
    render(body, exchange, 200)
  }
}


class AdminService(config: ConfigMap, runtime: RuntimeEnvironment) {
  val adminHttp = new AdminHttpService(config, runtime)
  adminHttp.addContext("/status/", new StatusReportHandler())
  adminHttp.start()
}
