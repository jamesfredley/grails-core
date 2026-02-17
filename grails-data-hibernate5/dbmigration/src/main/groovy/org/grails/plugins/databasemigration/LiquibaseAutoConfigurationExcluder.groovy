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
package org.grails.plugins.databasemigration

import groovy.transform.CompileStatic
import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata

/**
 * Automatically excludes Spring Boot's {@code LiquibaseAutoConfiguration} when the
 * Grails Database Migration Plugin is on the classpath.
 *
 * <p>Spring Boot auto-configures its own Liquibase instance via
 * {@code LiquibaseAutoConfiguration}, which creates a {@code SpringLiquibase} bean
 * that conflicts with the plugin's own {@link DatabaseMigrationGrailsPlugin#doWithApplicationContext()}
 * lifecycle management. Without this exclusion, both Spring Boot and the plugin
 * attempt to run Liquibase migrations, causing duplicate changelog execution,
 * lock contention on the {@code DATABASECHANGELOGLOCK} table, or startup failures.</p>
 *
 * <p>Previously, users had to manually add {@code spring.liquibase.enabled: false}
 * to {@code application.yml}. This filter removes that requirement by automatically
 * filtering out the conflicting auto-configuration during Spring Boot's
 * auto-configuration discovery phase.</p>
 *
 * <p>Registered via {@code META-INF/spring.factories} as an
 * {@link AutoConfigurationImportFilter}. This runs before auto-configuration
 * bytecode is loaded, so there is no performance overhead from excluded classes.</p>
 *
 * @since 7.0.8
 * @see AutoConfigurationImportFilter
 * @see DatabaseMigrationGrailsPlugin
 */
@CompileStatic
class LiquibaseAutoConfigurationExcluder implements AutoConfigurationImportFilter {

    /**
     * Spring Boot Liquibase auto-configuration class that conflicts with the
     * Grails Database Migration Plugin. Excluded unconditionally when the
     * plugin is on the classpath.
     */
    private static final Set<String> EXCLUDED_AUTO_CONFIGURATIONS = [
            'org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration',
    ] as Set<String>

    @Override
    boolean[] match(String[] autoConfigurationClasses, AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] matches = new boolean[autoConfigurationClasses.length]
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            matches[i] = !EXCLUDED_AUTO_CONFIGURATIONS.contains(autoConfigurationClasses[i])
        }
        return matches
    }

    /**
     * Returns the set of auto-configuration class names that this filter excludes.
     * Exposed for testing and diagnostic purposes.
     *
     * @return unmodifiable set of excluded class names
     */
    static Set<String> getExcludedAutoConfigurations() {
        return Collections.unmodifiableSet(EXCLUDED_AUTO_CONFIGURATIONS)
    }
}
