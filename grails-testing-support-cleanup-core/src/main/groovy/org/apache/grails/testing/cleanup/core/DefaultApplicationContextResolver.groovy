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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import org.spockframework.runtime.extension.IMethodInvocation

import org.springframework.context.ApplicationContext

/**
 * Default implementation of {@link ApplicationContextResolver} that resolves the
 * {@link ApplicationContext} from a Spock test instance via Groovy property access.
 *
 * <p>Since {@link DatabaseCleanup @DatabaseCleanup} requires {@code @Integration}, the
 * {@code IntegrationTestAstTransformation} ensures that test classes implement
 * {@code ApplicationContextAware} and have a {@code setApplicationContext()} method
 * injected at compile time. This means the {@code applicationContext} property is always
 * available as a standard Groovy property on the test instance.</p>
 *
 * @see ApplicationContextResolver
 * @see DatabaseCleanup#resolver()
 */
@Slf4j
@CompileStatic
class DefaultApplicationContextResolver implements ApplicationContextResolver {

    @Override
    ApplicationContext resolve(IMethodInvocation invocation) {
        resolveApplicationContext(invocation)
    }

    /**
     * Resolves the ApplicationContext from the test instance via Groovy property access.
     *
     * <p>The {@code @Integration} AST transformation ensures that all integration test
     * instances have an {@code applicationContext} property available.</p>
     *
     * @throws IllegalStateException if the ApplicationContext cannot be resolved
     */
    @CompileDynamic
    private static ApplicationContext resolveApplicationContext(IMethodInvocation invocation) {
        Object instance = invocation.instance

        if (instance && instance.hasProperty('applicationContext')) {
            try {
                Object ctx = instance.applicationContext
                if (ctx instanceof ApplicationContext) {
                    return (ApplicationContext) ctx
                }
            }
            catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to read applicationContext property from test instance of type ${instance.getClass().name}" as String, e)
            }
        }

        String instanceType = instance ? instance.getClass().name : 'null'
        throw new IllegalStateException(
            "Could not resolve ApplicationContext from test instance (type: ${instanceType}). Ensure the spec is annotated with @Integration so the applicationContext property is available." as String)
    }
}
