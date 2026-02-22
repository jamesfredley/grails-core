/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.grails.testing.cleanup.h2

import java.sql.Connection
import java.sql.DatabaseMetaData

import javax.sql.DataSource

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Helper utility for H2 database cleanup operations. Provides H2-specific logic such as
 * resolving the schema name from a {@link DataSource} by inspecting JDBC connection metadata
 * and parsing H2 JDBC URLs.
 */
@Slf4j
@CompileStatic
class H2DatabaseCleanupHelper {

    /**
     * Resolves the schema name for the given H2 datasource by inspecting the JDBC connection metadata.
     * For H2, the schema name corresponds to the uppercase database name from the JDBC URL
     * (e.g., {@code jdbc:h2:mem:testDb} results in schema {@code TESTDB}).
     *
     * @param dataSource the datasource to resolve the schema for
     * @return the schema name, or {@code null} if it cannot be determined
     */
    static String resolveSchemaName(DataSource dataSource) {
        Connection connection = null
        try {
            connection = dataSource.getConnection()
            String schema = connection.getSchema()
            if (schema) {
                log.debug('Resolved schema name from connection: {}', schema)
                return schema
            }

            // Fallback: try to get the schema from the database metadata URL
            DatabaseMetaData metaData = connection.getMetaData()
            String url = metaData.getURL()
            if (url) {
                schema = extractSchemaFromUrl(url)
                log.debug('Resolved schema name from URL {}: {}', url, schema)
                return schema
            }
        }
        catch (Exception e) {
            log.warn('Failed to resolve schema name from datasource', e)
        }
        finally {
            if (connection) {
                try {
                    connection.close()
                }
                catch (Exception ignored) {
                    // ignore
                }
            }
        }
        null
    }

    /**
     * Extracts the database name from an H2 JDBC URL and converts it to the uppercase
     * schema name that H2 uses.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code jdbc:h2:mem:testDb} &rarr; {@code TESTDB}</li>
     *   <li>{@code jdbc:h2:mem:grailsDB} &rarr; {@code GRAILSDB}</li>
     *   <li>{@code jdbc:h2:mem:testDb;LOCK_TIMEOUT=10000} &rarr; {@code TESTDB}</li>
     * </ul>
     *
     * @param url the JDBC URL
     * @return the uppercase schema name, or {@code null} if the URL format is not recognized
     */
    static String extractSchemaFromUrl(String url) {
        if (!url) {
            return null
        }

        // Handle H2 in-memory URLs: jdbc:h2:mem:dbName or jdbc:h2:mem:dbName;params
        if (url.startsWith('jdbc:h2:mem:')) {
            String remainder = url.substring('jdbc:h2:mem:'.length())
            // Remove any trailing parameters after ';'
            int semicolonIdx = remainder.indexOf(';')
            if (semicolonIdx >= 0) {
                remainder = remainder.substring(0, semicolonIdx)
            }
            return remainder ? remainder.toUpperCase() : null
        }

        // Handle H2 file-based URLs: jdbc:h2:./dbName, jdbc:h2:file:./path/dbName, jdbc:h2:tcp://host/dbName
        if (url.startsWith('jdbc:h2:')) {
            String remainder = url.substring('jdbc:h2:'.length())

            // Remove protocol prefixes (tcp://, ssl://)
            if (remainder.startsWith('tcp://') || remainder.startsWith('ssl://')) {
                int slashIdx = remainder.indexOf('/', remainder.indexOf('//') + 2)
                if (slashIdx >= 0) {
                    remainder = remainder.substring(slashIdx + 1)
                }
            }

            // Remove 'file:' prefix
            if (remainder.startsWith('file:')) {
                remainder = remainder.substring('file:'.length())
            }

            // Remove any trailing parameters after ';'
            int semicolonIdx = remainder.indexOf(';')
            if (semicolonIdx >= 0) {
                remainder = remainder.substring(0, semicolonIdx)
            }

            // Get the last path segment as the database name
            int lastSlash = remainder.lastIndexOf('/')
            if (lastSlash >= 0) {
                remainder = remainder.substring(lastSlash + 1)
            }

            return remainder ? remainder.toUpperCase() : null
        }

        null
    }
}
