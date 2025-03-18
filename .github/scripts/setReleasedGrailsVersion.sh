#!/bin/bash

set -e

echo "Updating Grails Tests to Grails Version ${RELEASE_VERSION}"
sed -i "s/assertEquals(\".*$/assertEquals(\"${RELEASE_VERSION}\", GrailsUtil.getGrailsVersion());/" "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"
sed -n "/assertEquals(\".*/p" "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"
git add "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"