#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$JAR" ]; then
  echo "Gradle wrapper jar no encontrado. Descargando wrapper oficial Gradle 8.9..."
  if command -v curl >/dev/null 2>&1; then
    curl -L --fail -o "$JAR" https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar || exit 1
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$JAR" https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar || exit 1
  else
    echo "Instala Gradle 8.9 o abre el proyecto directamente con Android Studio."
    exit 1
  fi
fi
exec java -Dorg.gradle.appname=gradlew -classpath "$JAR" org.gradle.wrapper.GradleWrapperMain "$@"
