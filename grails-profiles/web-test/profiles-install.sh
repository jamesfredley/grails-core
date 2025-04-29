#!/bin/bash
set -e

EXIT_STATUS=0

curl -s get.sdkman.io | bash

source "$HOME/.sdkman/bin/sdkman-init.sh"

echo sdkman_auto_answer=true > ~/.sdkman/etc/config

source "$HOME/.sdkman/bin/sdkman-init.sh"

./gradlew build --console=plain || EXIT_STATUS=$?

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

cd build/grails-wrapper/

./gradlew assemble || EXIT_STATUS=$?

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

cd ../../

mkdir -p $HOME/.grails/wrapper

cp $GITHUB_WORKSPACE/build/grails-wrapper/wrapper/build/libs/grails5-wrapper-3.0.0-SNAPSHOT.jar $HOME/.grails/wrapper/grails5-wrapper.jar

sdk install grails dev $GITHUB_WORKSPACE/build/grails-core

sdk install grails

sdk use grails dev

grails --version

grails create-app demo.webapp

exit $EXIT_STATUS
