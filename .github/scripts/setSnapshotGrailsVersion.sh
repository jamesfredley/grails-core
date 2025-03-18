#!/bin/bash

set -e

echo "Setting new Grails snapshot version"
sed -i "s/assertEquals(\".*$/assertEquals(\"${NEXT_VERSION}-SNAPSHOT\", GrailsUtil.getGrailsVersion());/" "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"
sed -n "/assertEquals(\".*/p" "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"
git add "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"
