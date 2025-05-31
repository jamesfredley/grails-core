#!/usr/bin/env bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

# This file assumes the gnu version of coreutils is installed, which is not installed by default on a mac
set -e

DOWNLOAD_LOCATION="${1:-downloads}"
DOWNLOAD_LOCATION=$(realpath "${DOWNLOAD_LOCATION}")
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

CWD=$(pwd)

cd "${DOWNLOAD_LOCATION}/grails"

mkdir -p "${DOWNLOAD_LOCATION}/grails/etc/bin/results"
if [[ -f "${DOWNLOAD_LOCATION}/grails/CHECKSUMS" ]]; then
  echo "✅ File 'CHECKSUMS' exists."
else
  echo "❌ File 'CHECKSUMS' not found. Grails Source Distributions should have a CHECKSUMS file at the root..."
  exit 1
fi

if [[ -f "${DOWNLOAD_LOCATION}/grails/BUILD_DATE" ]]; then
  echo "✅ File 'BUILD_DATE' exists."
else
  echo "❌ File 'BUILD_DATE' not found. Grails Source Distributions should have a BUILD_DATE file at the root..."
  exit 1
fi
export SOURCE_DATE_EPOCH=$(cat "${DOWNLOAD_LOCATION}/grails/BUILD_DATE")

if [[ -d "${DOWNLOAD_LOCATION}/grails/etc/bin/results/first" ]]; then
  echo "✅ Directory 'first' exists."
else
  echo "❌ Directory 'first' not found. Please place the published jar files under ${DOWNLOAD_LOCATION}/grails/etc/bin/results/first..."
  exit 1
fi

killall -e java || true
cd grails-gradle
./gradlew build --rerun-tasks -PskipTests --no-build-cache
cd ..
./gradlew build --rerun-tasks -PskipTests --no-build-cache
"${SCRIPT_DIR}/generate-build-artifact-hashes.groovy" > "${DOWNLOAD_LOCATION}/grails/etc/bin/results/second.txt"

## Flatten the jar files since our published artifacts are flat
tmpfile=$(mktemp)
while read -r filepath checksum; do
  echo "$(basename "$filepath") $checksum"
done < "${DOWNLOAD_LOCATION}/grails/etc/bin/results/second.txt" > "$tmpfile" && mv "$tmpfile" "${DOWNLOAD_LOCATION}/grails/etc/bin/results/second.txt"

# filter to only published jars to compare against
grep -Ff "${DOWNLOAD_LOCATION}/grails/CHECKSUMS" "${DOWNLOAD_LOCATION}/grails/etc/bin/results/second.txt" > "${DOWNLOAD_LOCATION}/grails/etc/bin/results/filtered.txt"
rm -f "${DOWNLOAD_LOCATION}/grails/etc/bin/results/second.txt"
mv "${DOWNLOAD_LOCATION}/grails/etc/bin/results/filtered.txt" "${DOWNLOAD_LOCATION}/grails/etc/bin/results/second.txt"

mkdir -p "${DOWNLOAD_LOCATION}/grails/etc/bin/results/second"
find . -path ./etc -prune -o -type f -path '*/build/libs/*.jar' ! -name "buildSrc.jar" -exec cp -t "${DOWNLOAD_LOCATION}/grails/etc/bin/results/second/" -- {} +

cd "${DOWNLOAD_LOCATION}/grails/etc/bin/results"

# diff -u CHECKSUMS second.txt
DIFF_RESULTS=$(comm -3 <(sort ../../../CHECKSUMS) <(sort second.txt) | cut -d' ' -f1 | sed 's/^[[:space:]]*//;s/[[:space:]]*$//' | uniq | sort)
echo "Differing artifacts:"
echo "$DIFF_RESULTS" > diff.txt
cat diff.txt

printf '%s\n' "$DIFF_RESULTS" | sed 's|^etc/bin/results/||' > toPurge.txt
find first -type f -name '*.jar' -print | sed 's|^first/||' | grep -F -x -v -f toPurge.txt |
  while IFS= read -r f; do
    rm -f "./first/$f"
  done
find second -type f -name '*.jar' -print | sed 's|^second/||' | grep -F -x -v -f toPurge.txt |
  while IFS= read -r f; do
    rm -f "./second/$f"
  done
rm toPurge.txt
find . -type d -empty -delete
cd "$CWD"
