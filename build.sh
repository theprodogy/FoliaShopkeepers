#!/usr/bin/env bash
# Abort on error
set -e

pushd "$(dirname "$BASH_SOURCE")"
trap popd EXIT

BUILD_TYPE="full"
# Optional argument 'minimal': Only builds the api project.
if [ $# -eq 1 ] && [ "$1" = "minimal" ]; then
    echo "Minimal build: Only building the api project."
    BUILD_TYPE="minimal"
fi

# We require Java 21 to build:
source scripts/installJDK.sh 21

# Build via Gradle:
if [ "$BUILD_TYPE" = "minimal" ]; then
    ./gradlew cleanInstallMinimal --exclude-task test
else
    ./gradlew cleanInstall
fi
