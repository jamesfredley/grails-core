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
package app5

import spock.lang.Specification

import grails.testing.mixin.integration.Integration
import org.apache.grails.testing.http.client.HttpClientSupport

@Integration
class WildcardValidationDisabledSpec extends Specification implements HttpClientSupport {

    def 'disabled wildcard validation preserves the first wildcard match for non-action segments'() {
        when: 'requesting a numeric segment that would otherwise fall through to the wildcard id mapping'
        def response = http('/wildcard-disabled/42')

        then: 'the first wildcard action mapping remains selected and the request is rejected'
        response.assertStatus(404)
    }

    def 'disabled wildcard validation does not make the target controller action unavailable'() {
        when: 'requesting the same action through an explicit literal route'
        def response = http('/wildcard-disabled/show-action')

        then: 'the controller action itself still responds successfully'
        response.assertJson(200, [
            controller: 'wildcardValidation',
            action: 'show'
        ])
    }
}
