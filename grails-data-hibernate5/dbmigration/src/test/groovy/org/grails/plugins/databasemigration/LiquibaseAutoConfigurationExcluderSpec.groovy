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

import org.springframework.boot.autoconfigure.AutoConfigurationImportFilter
import org.springframework.boot.autoconfigure.AutoConfigurationMetadata
import org.springframework.core.io.support.SpringFactoriesLoader
import spock.lang.Specification

class LiquibaseAutoConfigurationExcluderSpec extends Specification {

    LiquibaseAutoConfigurationExcluder excluder = new LiquibaseAutoConfigurationExcluder()

    def "excludes LiquibaseAutoConfiguration"() {
        given:
        String[] candidates = [
                'org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration',
                'org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration',
                'org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration',
        ] as String[]

        when:
        boolean[] matches = excluder.match(candidates, Stub(AutoConfigurationMetadata))

        then:
        !matches[0]  // LiquibaseAutoConfiguration excluded
        matches[1]   // DataSourceAutoConfiguration allowed
        matches[2]   // HibernateJpaAutoConfiguration allowed
    }

    def "allows all non-Liquibase auto-configurations"() {
        given:
        String[] candidates = [
                'org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration',
                'org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration',
                'org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration',
        ] as String[]

        when:
        boolean[] matches = excluder.match(candidates, Stub(AutoConfigurationMetadata))

        then:
        matches.every { it }
    }

    def "handles empty candidate list"() {
        given:
        String[] candidates = [] as String[]

        when:
        boolean[] matches = excluder.match(candidates, Stub(AutoConfigurationMetadata))

        then:
        matches.length == 0
    }

    def "handles null entries in candidate list"() {
        given:
        String[] candidates = [null, 'org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration', null] as String[]

        when:
        boolean[] matches = excluder.match(candidates, Stub(AutoConfigurationMetadata))

        then:
        matches[0]   // null is not in excluded set, so allowed
        !matches[1]  // LiquibaseAutoConfiguration excluded
        matches[2]   // null allowed
    }

    def "excluded set contains exactly LiquibaseAutoConfiguration"() {
        expect:
        LiquibaseAutoConfigurationExcluder.excludedAutoConfigurations == [
                'org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration',
        ] as Set
    }

    def "excluded set is unmodifiable"() {
        when:
        LiquibaseAutoConfigurationExcluder.excludedAutoConfigurations.add('something')

        then:
        thrown(UnsupportedOperationException)
    }

    def "registered in spring.factories as AutoConfigurationImportFilter"() {
        when:
        List<AutoConfigurationImportFilter> filters = SpringFactoriesLoader.loadFactories(
                AutoConfigurationImportFilter, getClass().classLoader
        )

        then:
        filters.any { it instanceof LiquibaseAutoConfigurationExcluder }
    }
}
