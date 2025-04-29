#!/bin/bash
set -e

EXIT_STATUS=0

mkdir multi

mv webapp multi/webapp

mv multi/webapp/gradle multi/gradle

mv multi/webapp/gradlew multi/gradlew

mv multi/webapp/gradlew.bat multi/gradlew.bat

cp -r functional-tests multi/

cp runtests.sh multi/

cd multi

touch settings.gradle

echo "include 'webapp', 'functional-tests'" >> settings.gradle

cd webapp

./grailsw assemble || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw package || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw war || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw bug-report || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw clean || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw compile || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-command awesome || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-domain-class Book || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-script scripto || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-service BookService || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-unit-test Foo || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw dependency-report || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw list-plugins || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw plugin-info geb || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw stats || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-controller Company || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-integration-test Int || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-interceptor Company || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

./grailsw create-taglib Currency || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

cd ..

cd ..

./gradlew fixTests || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

cd multi/webapp

./grailsw test-app || EXIT_STATUS=$? 

if [ $EXIT_STATUS -ne 0 ]; then
  exit $EXIT_STATUS
fi

cd ..

./runtests.sh

cd ..

rm -rf multi

exit $EXIT_STATUS
