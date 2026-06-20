#!/bin/sh
APP_HOME=$(cd "${0%/*}" && pwd -P)
DEFAULT_JVM_OPTS='"--add-opens=java.base/java.util=ALL-UNNAMED" "--add-opens=java.base/java.io=ALL-UNNAMED" "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED" "--add-opens=java.base/java.net=ALL-UNNAMED" "--add-opens=java.base/java.nio=ALL-UNNAMED" "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED" "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED" "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED"'
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVACMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"
eval set -- $DEFAULT_JVM_OPTS '"$JAVA_OPTS"' '"$GRADLE_OPTS"' '"-Dorg.gradle.appname=${0##*/}"' -classpath '"$CLASSPATH"' org.gradle.wrapper.GradleWrapperMain '"$@"'
exec "$JAVACMD" "$@"
