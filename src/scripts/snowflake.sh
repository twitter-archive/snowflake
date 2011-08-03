#!/bin/sh
#
# snowflake init.d script.
#
# Snowflake, and all new java services, require the same directory structure
#   /usr/local/snowflake should contain 'releases' directory and be able to create a symlink
#   /var/log/snowflake (chown daemon, chmod 775)

APP_NAME="snowflake"
MAIN_JAR="snowflake-1.0.jar"
VERSION="1.0"
APP_HOME="/usr/local/snowflake/current"
MAIN_CLASS="com.twitter.service.snowflake.SnowflakeServer"
DAEMON="/usr/local/bin/daemon"

HEAP_OPTS="-Xmx700m -Xms700m -Xmn500m"
JMX_OPTS="-Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
GC_OPTS="-XX:+UseConcMarkSweepGC -verbosegc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -XX:+PrintGCDateStamps -XX:+UseParNewGC -Xloggc:/var/log/snowflake/gc.log"
DEBUG_OPTS="-XX:ErrorFile=/var/log/$APP_NAME/java_error%p.log"
JAVA_OPTS="-server $GC_OPTS $JMX_OPTS $HEAP_OPTS $DEBUG_OPTS"
JAVA_HOME=/usr/java/default

pidfile="/var/run/$APP_NAME/$APP_NAME.pid"
daemon_args="--name $APP_NAME --pidfile $pidfile"
daemon_start_args="--stdout=/var/log/$APP_NAME/stdout --stderr=/var/log/$APP_NAME/stderr"

function running() {
  $DAEMON $daemon_args --running
}

function find_java() {
  if [ ! -z "$JAVA_HOME" ]; then
    return
  fi
  potential=$(ls -r1d /opt/jdk /System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home /usr/java/default /usr/java/j* 2>/dev/null)
  for p in $potential; do
    if [ -x $p/bin/java ]; then
      JAVA_HOME=$p
      break
    fi
  done
}

find_java


case "$1" in
  start)
    echo -n "Starting $APP_NAME... "

    if [ ! -r $APP_HOME/$MAIN_JAR ]; then
      echo "FAIL"
      echo "*** $APP_NAME jar missing i $APP_HOME/$APP_NAME-$VERSION.jar - not starting"
      exit 1
    fi
    if [ ! -x $JAVA_HOME/bin/java ]; then
      echo "FAIL"
      echo "*** $JAVA_HOME/bin/java doesn't exist -- check JAVA_HOME?"
      exit 1
    fi
    if running; then
      echo "already running."
      exit 0
    fi

    $DAEMON $daemon_args $daemon_start_args -- ${JAVA_HOME}/bin/java ${JAVA_OPTS} -cp ${APP_HOME}/${MAIN_JAR} ${MAIN_CLASS} -f ${APP_HOME}/config/production.scala
    tries=0
    while ! running; do
      tries=$((tries + 1))
      if [ $tries -ge 5 ]; then
        echo "FAIL"
        exit 1
      fi
      sleep 1
    done
    echo "done."
  ;;

  stop)
    echo -n "Stopping $APP_NAME... "
    if ! running; then
      echo "wasn't running."
      exit 0
    fi

    kill -TERM $(cat $pidfile)
    tries=0
    while running; do
      tries=$((tries + 1))
      if [ $tries -ge 5 ]; then
        echo "FAIL"
        exit 1
      fi
      sleep 1
    done
    echo "done."
  ;;
  
  status)
    if running; then
      echo "$APP_NAME is running."
    else
      echo "$APP_NAME is NOT running."
    fi
  ;;

  restart)
    $0 stop
    sleep 2
    $0 start
  ;;

  *)
    echo "Usage: /etc/init.d/$APP_NAME {start|stop|restart|status}"
    exit 1
  ;;
esac

exit 0
