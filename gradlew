#!/bin/sh
if [ -d gradle/wrapper ]; then
    exec java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain "$@"
fi
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi
echo "Error: Gradle wrapper not found. Please install Gradle."
exit 1
