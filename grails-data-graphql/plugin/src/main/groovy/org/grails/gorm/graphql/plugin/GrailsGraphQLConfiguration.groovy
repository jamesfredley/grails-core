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

package org.grails.gorm.graphql.plugin

import grails.config.Config
import grails.core.GrailsApplication
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.ConfigurationProperties

import jakarta.annotation.PostConstruct

@CompileStatic
@ConfigurationProperties(prefix = 'grails.gorm.graphql')
class GrailsGraphQLConfiguration {

    // Kept in sync with AbstractDataBindingGrailsPlugin.DEFAULT_DATE_FORMATS in Grails 7.1.0.
    // Inlined to avoid coupling to a class whose location has shifted between Grails
    // releases (was DataBindingGrailsPlugin in 6.x).
    private static final List<String> DEFAULT_DATE_FORMATS = [
            'yyyy-MM-dd HH:mm:ss.S',
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            'yyyy-MM-dd HH:mm:ss.S z',
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            'HH:mm:ssZ',
            "yyyy-MM-dd'T'HH:mm:ss",
            'yyyy-MM-dd',
            'HH:mm:ss'
    ]

    @Autowired
    private GrailsApplication grailsApplication

    Boolean enabled = true

    List<String> dateFormats

    Boolean dateFormatLenient

    Map<String, Class> listArguments

    Boolean browser

    @PostConstruct
    void init() {
        Config config = grailsApplication.config
        if (dateFormats == null) {
            dateFormats = (List<String>) config.getProperty('grails.databinding.dateFormats', List, DEFAULT_DATE_FORMATS)
        }
        if (dateFormatLenient == null) {
            dateFormatLenient = config.getProperty('grails.databinding.dateParsingLenient', Boolean, false)
        }
        if (browser == null) {
            browser = false
        }
    }
}
