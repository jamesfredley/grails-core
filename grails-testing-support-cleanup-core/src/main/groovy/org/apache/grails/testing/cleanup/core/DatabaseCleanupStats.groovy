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

package org.apache.grails.testing.cleanup.core

import groovy.transform.CompileStatic

/**
 * Captures statistics from a single datasource cleanup operation, including which
 * tables were truncated and how many rows were in each table before truncation.
 *
 * <p>The {@link #datasourceName} field identifies which datasource these stats belong to.
 * The {@link #tableRowCounts} map tracks per-table row counts using a default of {@code 0L}
 * for unknown keys, enabling convenient {@code +=} accumulation.</p>
 *
 * <p>When debug output is enabled via the {@code grails.testing.cleanup.debug} system property,
 * the {@link #toFormattedReport()} method produces a human-readable summary suitable for
 * printing to the console.</p>
 */
@CompileStatic
class DatabaseCleanupStats {

    /**
     * System property that enables debug output of cleanup statistics to stdout.
     * Set to {@code "true"} to enable: {@code -Dgrails.testing.cleanup.debug=true}
     */
    static final String DEBUG_PROPERTY = 'grails.testing.cleanup.debug'

    /**
     * The name of the datasource bean that was cleaned up (e.g., "dataSource").
     * May be {@code null} if not set by the caller.
     */
    String datasourceName

    /**
     * Map of table names to the number of rows that existed before truncation.
     * Uses a default value of {@code 0L} for unknown keys, enabling {@code +=} accumulation.
     */
    Map<String, Long> tableRowCounts = [:].withDefault { 0L }

    /**
     * Returns {@code true} if the {@code grails.testing.cleanup.debug} system property
     * is set to {@code "true"}.
     */
    static boolean isDebugEnabled() {
        Boolean.getBoolean(DEBUG_PROPERTY)
    }

    /**
     * Returns a human-readable formatted report of the cleanup statistics.
     *
     * <p>Example output:</p>
     * <pre>
     * ==========================================================
     * Database Cleanup Stats (datasource: dataSource)
     * ==========================================================
     * Table Name                          | Row Count
     * ----------------------------------------------------------
     * BOOK                                | 5
     * AUTHOR                              | 3
     * ==========================================================
     * </pre>
     *
     * @return the formatted report string
     */
    String toFormattedReport() {
        String separator = '=========================================================='
        String divider = '----------------------------------------------------------'

        StringBuilder sb = new StringBuilder()
        sb.append(separator).append('\n')
        if (datasourceName) {
            sb.append("Database Cleanup Stats (datasource: ${datasourceName})").append('\n')
        }
        else {
            sb.append('Database Cleanup Stats').append('\n')
        }
        sb.append(separator).append('\n')

        if (tableRowCounts.isEmpty()) {
            sb.append('No tables were truncated.').append('\n')
        }
        else {
            sb.append(String.format('%-36s| %s', 'Table Name', 'Row Count')).append('\n')
            sb.append(divider).append('\n')
            tableRowCounts.each { String name, Long count ->
                sb.append(String.format('%-36s| %d', name, count)).append('\n')
            }
        }

        sb.append(separator).append('\n')
        sb.toString()
    }
}
