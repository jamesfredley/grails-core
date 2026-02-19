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
package org.grails.compiler.injection

import spock.lang.Specification

class ApplicationClassInjectorSpec extends Specification {

    def "EXCLUDED_AUTO_CONFIGURE_CLASSES contains expected entries"() {
        expect:
        ApplicationClassInjector.EXCLUDED_AUTO_CONFIGURE_CLASSES.contains(
                'org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration')
        ApplicationClassInjector.EXCLUDED_AUTO_CONFIGURE_CLASSES.contains(
                'org.springframework.boot.autoconfigure.reactor.ReactorAutoConfiguration')
        ApplicationClassInjector.EXCLUDED_AUTO_CONFIGURE_CLASSES.contains(
                'org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration')
    }

    def "CONDITIONAL_EXCLUSIONS contains LiquibaseAutoConfiguration entry"() {
        when:
        def exclusion = ApplicationClassInjector.CONDITIONAL_EXCLUSIONS.find {
            it.excludeClass == 'org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration'
        }

        then:
        exclusion != null
        exclusion.pluginClass == 'org.grails.plugins.databasemigration.DatabaseMigrationGrailsPlugin'
        exclusion.systemProperty == 'grails.dbmigration.excludeLiquibaseAutoConfiguration'
    }

    def "system property defaults to true (exclusion enabled)"() {
        given:
        def prop = 'grails.dbmigration.excludeLiquibaseAutoConfiguration'

        when:
        def previousValue = System.getProperty(prop)
        System.clearProperty(prop)

        then:
        Boolean.parseBoolean(System.getProperty(prop, 'true'))

        cleanup:
        if (previousValue != null) {
            System.setProperty(prop, previousValue)
        }
    }

    def "system property set to false disables exclusion"() {
        given:
        def prop = 'grails.dbmigration.excludeLiquibaseAutoConfiguration'
        def previousValue = System.getProperty(prop)

        when:
        System.setProperty(prop, 'false')

        then:
        !Boolean.parseBoolean(System.getProperty(prop, 'true'))

        cleanup:
        if (previousValue != null) {
            System.setProperty(prop, previousValue)
        } else {
            System.clearProperty(prop)
        }
    }

    def "shouldInject returns false for null URL"() {
        given:
        def injector = new ApplicationClassInjector()

        expect:
        !injector.shouldInject(null)
    }

    def "artefact types contains Application"() {
        given:
        def injector = new ApplicationClassInjector()

        expect:
        injector.artefactTypes.contains('Application')
    }
}
