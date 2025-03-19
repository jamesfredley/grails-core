#!/bin/bash

set -e

echo "Setting new version in GrailsUtilsTests.java: ${RELEASE_VERSION}"
sed -i "s/assertEquals(\".*$/assertEquals(\"${RELEASE_VERSION}\", GrailsUtil.getGrailsVersion());/" "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"
sed -n "/assertEquals(\".*/p" "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"
git add "${GITHUB_WORKSPACE}/grails-core/src/test/groovy/grails/util/GrailsUtilTests.java"