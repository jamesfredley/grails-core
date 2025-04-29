#!/bin/bash
set -e
EXIT_STATUS=0

start=`date +%s`

echo "Starting client and server"
./gradlew bootRun -parallel --console=plain &
PID1=$!

echo "Waiting 50 seconds  for client and server to start"

sleep 50

echo "Executing tests"

./gradlew --rerun-tasks -Dgeb.env=chromeHeadless functional-tests:test --console=plain || EXIT_STATUS=$?

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./gradlew --rerun-tasks -Dgeb.env=firefoxHeadless functional-tests:test --console=plain || EXIT_STATUS=$?

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

killall -9 java

end=`date +%s`
runtime=$((end-start))
echo "execution took $runtime seconds"
