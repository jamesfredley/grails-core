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

import javax.sql.DataSource

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.springframework.context.ApplicationContext

/**
 * Context that holds the discovered {@link DatabaseCleaner} implementations and
 * provides access to the application's datasources for cleanup operations.
 *
 * <p>Multiple cleaners can be registered (one per database type). When performing cleanup,
 * the context matches each datasource to an appropriate cleaner using either an explicit
 * database type from the {@link DatasourceCleanupMapping} or auto-discovery via
 * {@link DatabaseCleaner#supports(DataSource)}. If a datasource is marked for cleanup
 * but no matching cleaner is found, an error is thrown.</p>
 */
@Slf4j
@CompileStatic
class DatabaseCleanupContext {

    private final List<DatabaseCleaner> cleaners
    private final Map<String, DatabaseCleaner> cleanersByType
    private ApplicationContext applicationContext

    DatabaseCleanupContext(List<DatabaseCleaner> cleaners) {
        this.cleaners = Collections.unmodifiableList(new ArrayList<>(cleaners))
        Map<String, DatabaseCleaner> typeMap = [:]
        for (DatabaseCleaner cleaner : cleaners) {
            typeMap.put(cleaner.databaseType(), cleaner)
        }
        this.cleanersByType = Collections.unmodifiableMap(typeMap)
    }

    /**
     * @return the list of discovered {@link DatabaseCleaner} implementations
     */
    List<DatabaseCleaner> getCleaners() {
        cleaners
    }

    /**
     * @return the Spring {@link ApplicationContext}, or null if not yet resolved
     */
    ApplicationContext getApplicationContext() {
        applicationContext
    }

    /**
     * Sets the application context. Called by the interceptor when the test's
     * Spring context becomes available.
     */
    void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext
    }

    /**
     * Performs cleanup on datasources found in the application context using the
     * provided mapping from the {@link DatabaseCleanup} annotation.
     *
     * <p>If the mapping specifies explicit database types for datasources, those types
     * are used to look up the cleaner directly. Otherwise, auto-discovery via
     * {@link DatabaseCleaner#supports(DataSource)} is used.</p>
     *
     * @param mapping the parsed annotation value describing which datasources to clean
     *        and optionally which cleaner types to use
     * @return a list of {@link DatabaseCleanupStats}, one per datasource cleaned
     * @throws IllegalStateException if a datasource marked for cleanup has no
     *         matching {@link DatabaseCleaner} on the classpath, if an explicitly
     *         specified database type has no matching cleaner, or if a specified
     *         datasource bean does not exist in the application context
     */
    List<DatabaseCleanupStats> performCleanup(DatasourceCleanupMapping mapping) {
        if (!applicationContext) {
            throw new IllegalStateException('Cannot perform database cleanup: ApplicationContext is not available')
        }
        if (cleaners.isEmpty()) {
            throw new IllegalStateException('Cannot perform database cleanup: no DatabaseCleaner implementations found')
        }

        Map<String, DataSource> allDataSources = applicationContext.getBeansOfType(DataSource)
        List<DatabaseCleanupStats> allStats = []

        if (mapping.cleanAll) {
            // Clean all datasources using auto-discovery
            allDataSources.each { String beanName, DataSource dataSource ->
                DatabaseCleaner cleaner = findCleanerFor(dataSource)
                if (!cleaner) {
                    throw new IllegalStateException(
                        "No DatabaseCleaner implementation found that supports datasource '${beanName}'. Ensure that a database-specific cleanup library (e.g., grails-testing-support-cleanup-h2) is on the classpath for each database type used in your tests. Available cleaners: ${cleaners.collect { it.databaseType() }}" as String)
                }

                log.debug('Cleaning up datasource: {} (using {} cleaner)', beanName, cleaner.databaseType())
                DatabaseCleanupStats stats = cleaner.cleanup(applicationContext, dataSource)
                stats.datasourceName = beanName
                if (stats.tableRowCounts) {
                    log.debug('Cleaned {} tables from datasource {}', stats.tableRowCounts.size(), beanName)
                }
                allStats.add(stats)
            }
        }
        else {
            // Clean specific datasources per the mapping entries
            for (DatasourceCleanupMapping.Entry entry : mapping.entries) {
                DataSource dataSource = allDataSources.get(entry.datasourceName)
                if (!dataSource) {
                    throw new IllegalStateException(
                        "Datasource '${entry.datasourceName}' specified in @DatabaseCleanup was not found in the application context. Available datasources: ${allDataSources.keySet()}" as String)
                }

                DatabaseCleaner cleaner
                if (entry.hasExplicitType()) {
                    cleaner = cleanersByType.get(entry.databaseType)
                    if (!cleaner) {
                        throw new IllegalStateException(
                            "No DatabaseCleaner found for database type '${entry.databaseType}' specified in @DatabaseCleanup for datasource '${entry.datasourceName}'. Available cleaner types: ${cleanersByType.keySet()}" as String)
                    }
                }
                else {
                    cleaner = findCleanerFor(dataSource)
                    if (!cleaner) {
                        throw new IllegalStateException(
                            "No DatabaseCleaner implementation found that supports datasource '${entry.datasourceName}'. Ensure that a database-specific cleanup library (e.g., grails-testing-support-cleanup-h2) is on the classpath, or specify the database type explicitly: '${entry.datasourceName}:type'. Available cleaners: ${cleaners.collect { it.databaseType() }}" as String)
                    }
                }

                log.debug('Cleaning up datasource: {} (using {} cleaner)', entry.datasourceName, cleaner.databaseType())
                DatabaseCleanupStats stats = cleaner.cleanup(applicationContext, dataSource)
                stats.datasourceName = entry.datasourceName
                if (stats.tableRowCounts) {
                    log.debug('Cleaned {} tables from datasource {}', stats.tableRowCounts.size(), entry.datasourceName)
                }
                allStats.add(stats)
            }
        }

        allStats
    }

    /**
     * Finds a {@link DatabaseCleaner} that supports the given datasource via auto-discovery.
     *
     * @param dataSource the datasource to match
     * @return the matching cleaner, or {@code null} if none supports it
     */
    private DatabaseCleaner findCleanerFor(DataSource dataSource) {
        for (DatabaseCleaner cleaner : cleaners) {
            if (cleaner.supports(dataSource)) {
                return cleaner
            }
        }
        null
    }
}
