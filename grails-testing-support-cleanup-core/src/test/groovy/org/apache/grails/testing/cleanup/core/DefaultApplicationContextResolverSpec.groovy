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

import spock.lang.Specification

import org.spockframework.runtime.extension.IMethodInvocation

import org.springframework.context.ApplicationContext

class DefaultApplicationContextResolverSpec extends Specification {

    def "resolve returns ApplicationContext from Groovy property"() {
        given:
        def appCtx = Mock(ApplicationContext)
        def resolver = new DefaultApplicationContextResolver()

        def invocation = Mock(IMethodInvocation) {
            getInstance() >> new InstanceWithProperty(applicationContext: appCtx)
        }

        when:
        ApplicationContext result = resolver.resolve(invocation)

        then:
        result.is(appCtx)
    }

    def "resolve throws when no ApplicationContext can be found"() {
        given:
        def resolver = new DefaultApplicationContextResolver()

        def invocation = Mock(IMethodInvocation) {
            getInstance() >> new InstanceWithNoContext()
        }

        when:
        resolver.resolve(invocation)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('Could not resolve ApplicationContext')
        ex.message.contains('InstanceWithNoContext')
    }

    def "resolve throws when instance is null"() {
        given:
        def resolver = new DefaultApplicationContextResolver()

        def invocation = Mock(IMethodInvocation) {
            getInstance() >> null
        }

        when:
        resolver.resolve(invocation)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('Could not resolve ApplicationContext')
        ex.message.contains('null')
    }

    def "resolve throws when applicationContext property is null"() {
        given:
        def resolver = new DefaultApplicationContextResolver()

        def invocation = Mock(IMethodInvocation) {
            getInstance() >> new InstanceWithProperty()
        }

        when:
        resolver.resolve(invocation)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('Could not resolve ApplicationContext')
    }

    def "resolver has a no-arg constructor"() {
        when:
        def resolver = DefaultApplicationContextResolver.getDeclaredConstructor().newInstance()

        then:
        resolver instanceof ApplicationContextResolver
    }

    // --- Helper classes ---

    static class InstanceWithProperty {
        ApplicationContext applicationContext
    }

    static class InstanceWithNoContext {
        String name = 'test'
    }
}
