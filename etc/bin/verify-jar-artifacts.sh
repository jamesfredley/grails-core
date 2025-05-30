#!/usr/bin/env bash
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

set -euo pipefail

STAGING_REPO_ID=$1
RELEASE_TAG=$2
DOWNLOAD_LOCATION="${3:-downloads}"
DOWNLOAD_LOCATION=$(realpath "${DOWNLOAD_LOCATION}")

if [ -z "${STAGING_REPO_ID}" ] || [ -z "${RELEASE_TAG}" ]; then
  echo "Usage: $0 [staging-repo-id] [release-tag] <optional download location>"
  exit 1
fi

VERSION=${RELEASE_TAG#v}

ARTIFACTS_FILE="${DOWNLOAD_LOCATION}/PUBLISHED_ARTIFACTS"
CHECKSUMS_FILE="${DOWNLOAD_LOCATION}/CHECKSUMS"

if [ ! -f "${ARTIFACTS_FILE}" ] || [ ! -f "${CHECKSUMS_FILE}" ]; then
  echo "Required files ${ARTIFACTS_FILE} and/or ${CHECKSUMS_FILE} not found."
  exit 1
fi


REPO_BASE_URL="https://repository.apache.org/content/repositories/${STAGING_REPO_ID}"

# Create a temporary directory to work in
WORK_DIR='build'
mkdir -p $WORK_DIR
echo "Using temp dir: $WORK_DIR"
cd "$WORK_DIR"

# Read each line from PUBLISHED_ARTIFACTS
while IFS= read -r line; do
  JAR_FILE=$(echo "${line}" | awk '{print $1}')
  [[ "${JAR_FILE}" != *.jar ]] && continue
  
  COORDINATES=$(echo "${line}" | awk '{print $2}')

  GROUP_ID=$(echo "${COORDINATES}" | cut -d: -f1 | tr '.' '/')
  ARTIFACT_ID=$(echo "${COORDINATES}" | cut -d: -f2)
  VERSION=$(echo "${COORDINATES}" | cut -d: -f3)
  CLASSIFIER=$(echo "${COORDINATES}" | cut -d: -f4-)

  if [[ -n "${CLASSIFIER}" ]]; then
    FILE_NAME="${ARTIFACT_ID}-${VERSION}-${CLASSIFIER}.jar"
  else
    FILE_NAME="${ARTIFACT_ID}-${VERSION}.jar"
  fi

  JAR_URL="${REPO_BASE_URL}/${GROUP_ID}/${ARTIFACT_ID}/${VERSION}/${FILE_NAME}"
  ASC_URL="${JAR_URL}.asc"

  echo "🔎 Checking artifact: ${FILE_NAME}"
  echo "... Downloading: ${JAR_URL}"
  curl -sSfLO "${JAR_URL}"

  echo "... Downloading signature: ${ASC_URL}"
  curl -sSfLO "${ASC_URL}"

  echo "... Verifying GPG signature..."
  gpg --verify "${FILE_NAME}.asc" "${FILE_NAME}"

  EXPECTED_CHECKSUM=$(grep "^${FILE_NAME} " "${CHECKSUMS_FILE}" | awk '{print $2}')
  if [ -z "${EXPECTED_CHECKSUM}" ]; then
    echo "❌ Checksum not found for ${FILE_NAME}"
    exit 1
  fi

  echo "... Verifying checksum..."
  ACTUAL_CHECKSUM=$(shasum -a 512 "${FILE_NAME}" | awk '{print $1}')

  if [ "${ACTUAL_CHECKSUM}" != "${EXPECTED_CHECKSUM}" ]; then
    echo "❌ Checksum mismatch for ${FILE_NAME}"
    echo "Expected: ${EXPECTED_CHECKSUM}"
    echo "Actual:   ${ACTUAL_CHECKSUM}"
    exit 1
  fi

  echo "✅ Verified: ${FILE_NAME}"
done < "${ARTIFACTS_FILE}"

echo "✅✅✅ All artifacts verified successfully. ✅✅✅"
