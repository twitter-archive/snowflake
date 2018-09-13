#!/bin/bash

APP_NAME="snowflake"
MAIN_JAR="snowflake-1.0.jar"
VERSION="1.0"
APP_HOME="/usr/local/snowflake/current"
MAIN_CLASS="com.twitter.service.snowflake.SnowflakeServer"

HEAP_OPTS="-Xmx700m -Xms700m -Xmn500m"
JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
GC_OPTS="-XX:+UseConcMarkSweepGC -verbosegc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+UseParNewGC -Xloggc:/var/log/snowflake/gc.log"
DEBUG_OPTS="-XX:ErrorFile=/var/log/$APP_NAME/java_error%p.log"
JAVA_OPTS="-server $GC_OPTS $JMX_OPTS $HEAP_OPTS $DEBUG_OPTS"
JAVA_HOME=/usr/java/default

${JAVA_HOME}/bin/java ${JAVA_OPTS} -cp ${APP_HOME}/${MAIN_JAR} ${MAIN_CLASS} -f ${APP_HOME}/config/production.scala