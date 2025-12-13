#!/usr/bin/env sh

##############################################################################
##
##  Gradle start up script for POSIX compatible systems
##
##############################################################################

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass them to the JVM.
DEFAULT_JVM_OPTS=""

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set to 1/4 of total memory
MAX_MEMORY=${MAX_MEMORY:-"1024m"}

# Attempt to locate the Java executable
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/bin/java" ] ; then
        JAVA_EXEC="$JAVA_HOME/bin/java"
    fi
fi

if [ -z "$JAVA_EXEC" ] ; then
    JAVA_EXEC=`which java`
fi

if [ -z "$JAVA_EXEC" ] ; then
    echo "ERROR: Java executable not found in your path or JAVA_HOME is not set." >&2
    exit 1
fi

# Determine location of Gradle distribution
if [ -z "$GRADLE_HOME" ] ; then
    # Try to determine GRADLE_HOME from the wrapper jar location
    GRADLE_HOME=`dirname "$0"`
fi

# Wrapper properties file location
GRADLE_WRAPPER_PROPERTIES="$GRADLE_HOME/gradle/wrapper/gradle-wrapper.properties"

# Check for and use the wrapper jar
if [ -f "$GRADLE_WRAPPER_PROPERTIES" ] ; then
    # Parse the distributionUrl for the wrapper jar path
    DISTRIBUTION_URL=`grep -E '^distributionUrl=' "$GRADLE_WRAPPER_PROPERTIES" | cut -d '=' -f 2- | sed 's/\\//g'`
    WRAPPER_JAR_PATH="$GRADLE_HOME/gradle/wrapper/gradle-wrapper.jar"
    
    # If the wrapper jar doesn't exist, the build will download it.
    if [ ! -f "$WRAPPER_JAR_PATH" ] ; then
        WRAPPER_JAR_PATH=`echo "$WRAPPER_JAR_PATH" | sed 's/\\//g'`
    fi

    # Execute the build using the wrapper jar
    exec "$JAVA_EXEC" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
        -Dgradle.user.home="$GRADLE_USER_HOME" \
        -classpath "$WRAPPER_JAR_PATH" \
        org.gradle.wrapper.GradleWrapperMain "$@"
else
    echo "ERROR: Gradle wrapper properties file not found at $GRADLE_WRAPPER_PROPERTIES" >&2
    exit 1
fi
