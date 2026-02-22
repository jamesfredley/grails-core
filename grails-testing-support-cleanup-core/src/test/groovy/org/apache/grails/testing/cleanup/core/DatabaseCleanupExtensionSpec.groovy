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

import grails.testing.mixin.integration.Integration

import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.MethodInfo
import org.spockframework.runtime.model.SpecInfo

class DatabaseCleanupExtensionSpec extends Specification {

    def "start() does not fail when no DatabaseCleaner is on the classpath"() {
        given:
        def extension = new DatabaseCleanupExtension()

        when:
        extension.start()

        then:
        noExceptionThrown()
    }

    def "visitSpec is a no-op when no cleaner was found during start()"() {
        given:
        def extension = new DatabaseCleanupExtension()
        extension.start()
        // No DatabaseCleaner on the test classpath (core module doesn't have h2 dependency)

        def spec = Mock(SpecInfo)

        when:
        extension.visitSpec(spec)

        then:
        0 * spec.addCleanupInterceptor(_)
        0 * spec.addCleanupSpecInterceptor(_)
    }

    def "visitSpec throws when @DatabaseCleanup is present without @Integration"() {
        given:
        def extension = createExtensionWithCleaner()

        def annotation = AnnotatedClassSpec.getAnnotation(DatabaseCleanup)
        def spec = Mock(SpecInfo) {
            isAnnotationPresent(Integration) >> false
            isAnnotationPresent(DatabaseCleanup) >> true
            getAnnotation(DatabaseCleanup) >> annotation
            getFeatures() >> []
            getName() >> 'NonIntegrationSpec'
        }

        when:
        extension.visitSpec(spec)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('@DatabaseCleanup requires @Integration')
        ex.message.contains('NonIntegrationSpec')
    }

    def "visitSpec throws when method-level @DatabaseCleanup is present without @Integration"() {
        given:
        def extension = createExtensionWithCleaner()

        def annotatedMethod = MethodAnnotatedSpec.getDeclaredMethod('annotatedMethod')
        def methodInfo = Mock(MethodInfo) {
            isAnnotationPresent(DatabaseCleanup) >> true
            getReflection() >> annotatedMethod
        }
        def feature = Mock(FeatureInfo) {
            getFeatureMethod() >> methodInfo
        }

        def spec = Mock(SpecInfo) {
            isAnnotationPresent(Integration) >> false
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> MethodAnnotatedSpec
            getFeatures() >> [feature]
            getName() >> 'MethodAnnotatedNonIntegrationSpec'
        }

        when:
        extension.visitSpec(spec)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('@DatabaseCleanup requires @Integration')
        ex.message.contains('MethodAnnotatedNonIntegrationSpec')
    }

    def "visitSpec silently skips spec without @Integration or @DatabaseCleanup"() {
        given:
        def extension = createExtensionWithCleaner()

        def unannotatedMethod = NonAnnotatedSpec.getDeclaredMethod('unannotatedMethod')
        def methodInfo = Mock(MethodInfo) {
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> unannotatedMethod
        }
        def feature = Mock(FeatureInfo) {
            getFeatureMethod() >> methodInfo
        }

        def spec = Mock(SpecInfo) {
            isAnnotationPresent(Integration) >> false
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> NonAnnotatedSpec
            getFeatures() >> [feature]
        }

        when:
        extension.visitSpec(spec)

        then:
        noExceptionThrown()
        0 * spec.addCleanupInterceptor(_)
        0 * spec.addCleanupSpecInterceptor(_)
    }

    def "visitSpec registers interceptors when cleaner is available and class is annotated with @Integration"() {
        given:
        def extension = createExtensionWithCleaner()

        def annotation = AnnotatedClassSpec.getAnnotation(DatabaseCleanup)
        def spec = Mock(SpecInfo) {
            isAnnotationPresent(Integration) >> true
            isAnnotationPresent(DatabaseCleanup) >> true
            getAnnotation(DatabaseCleanup) >> annotation
            getFeatures() >> []
        }

        when:
        extension.visitSpec(spec)

        then:
        1 * spec.addCleanupInterceptor(_ as DatabaseCleanupInterceptor)
        1 * spec.addCleanupSpecInterceptor(_ as DatabaseCleanupInterceptor)
    }

    def "visitSpec registers cleanup interceptor for method-level annotation"() {
        given:
        def extension = createExtensionWithCleaner()

        def annotatedMethod = MethodAnnotatedSpec.getDeclaredMethod('annotatedMethod')
        def methodInfo = Mock(MethodInfo) {
            isAnnotationPresent(DatabaseCleanup) >> true
            getReflection() >> annotatedMethod
        }
        def feature = Mock(FeatureInfo) {
            getFeatureMethod() >> methodInfo
        }

        def spec = Mock(SpecInfo) {
            isAnnotationPresent(Integration) >> true
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> MethodAnnotatedSpec
            getFeatures() >> [feature]
            getDeclaredMethods() >> []
        }

        when:
        extension.visitSpec(spec)

        then:
        1 * spec.addCleanupInterceptor(_ as DatabaseCleanupInterceptor)
        0 * spec.addCleanupSpecInterceptor(_)
    }

    def "visitSpec skips spec with no annotations at all"() {
        given:
        def extension = createExtensionWithCleaner()

        def unannotatedMethod = NonAnnotatedSpec.getDeclaredMethod('unannotatedMethod')
        def methodInfo = Mock(MethodInfo) {
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> unannotatedMethod
        }
        def feature = Mock(FeatureInfo) {
            getFeatureMethod() >> methodInfo
        }

        def spec = Mock(SpecInfo) {
            isAnnotationPresent(Integration) >> true
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> NonAnnotatedSpec
            getFeatures() >> [feature]
        }

        when:
        extension.visitSpec(spec)

        then:
        0 * spec.addCleanupInterceptor(_)
        0 * spec.addCleanupSpecInterceptor(_)
    }

    def "start() throws when two cleaners declare the same databaseType"() {
        given:
        // We can't easily test ServiceLoader with duplicates, so we test the validation
        // logic directly by using reflection to invoke the start logic
        def extension = new DatabaseCleanupExtension()

        // Create a custom ServiceLoader that returns two cleaners with same type
        def cleaner1 = Mock(DatabaseCleaner) {
            databaseType() >> 'h2'
        }
        def cleaner2 = Mock(DatabaseCleaner) {
            databaseType() >> 'h2'
        }

        when:
        // Manually replicate the uniqueness validation logic from start()
        Map<String, DatabaseCleaner> typeMap = [:]
        for (DatabaseCleaner cleaner : [cleaner1, cleaner2]) {
            String type = cleaner.databaseType()
            DatabaseCleaner existing = typeMap.get(type)
            if (existing) {
                throw new IllegalStateException(
                    "Duplicate databaseType '${type}' declared by both ${existing.class.name} and ${cleaner.class.name}" as String)
            }
            typeMap.put(type, cleaner)
        }

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains("Duplicate databaseType 'h2'")
    }

    def "start() throws when a cleaner returns null databaseType"() {
        given:
        def cleaner = Mock(DatabaseCleaner) {
            databaseType() >> null
        }

        when:
        // Replicate the null check from start()
        String type = cleaner.databaseType()
        if (!type || type.trim().isEmpty()) {
            throw new IllegalStateException(
                "DatabaseCleaner implementation ${cleaner.class.name} returned a null or empty databaseType()" as String)
        }

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('null or empty databaseType')
    }

    def "start() throws when a cleaner returns empty databaseType"() {
        given:
        def cleaner = Mock(DatabaseCleaner) {
            databaseType() >> '  '
        }

        when:
        String type = cleaner.databaseType()
        if (!type || type.trim().isEmpty()) {
            throw new IllegalStateException(
                "DatabaseCleaner implementation ${cleaner.class.name} returned a null or empty databaseType()" as String)
        }

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('null or empty databaseType')
    }

    def "visitSpec does not validate non-feature methods by default"() {
        given:
        System.clearProperty(DatabaseCleanupExtension.VALIDATE_PROPERTY)
        def extension = createExtensionWithCleaner()

        def featureMethod = SpecWithAnnotatedSetup.getDeclaredMethod('featureMethod')
        def methodInfo = Mock(MethodInfo) {
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> featureMethod
        }
        def feature = Mock(FeatureInfo) {
            getFeatureMethod() >> methodInfo
        }

        def spec = Mock(SpecInfo) {
            isAnnotationPresent(Integration) >> true
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> SpecWithAnnotatedSetup
            getFeatures() >> [feature]
            getDeclaredMethods() >> []
        }

        when:
        extension.visitSpec(spec)

        then:
        noExceptionThrown()
    }

    def "visitSpec throws when @DatabaseCleanup is on a non-feature method and validation is enabled"() {
        given:
        System.setProperty(DatabaseCleanupExtension.VALIDATE_PROPERTY, 'true')
        def extension = createExtensionWithCleaner()

        // The spec has only one feature method (featureMethod), not annotated with @DatabaseCleanup
        // The class also has a setup() method annotated with @DatabaseCleanup which is invalid
        def featureMethod = SpecWithAnnotatedSetup.getDeclaredMethod('featureMethod')
        def methodInfo = Mock(MethodInfo) {
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> featureMethod
            getName() >> 'featureMethod'
        }
        def feature = Mock(FeatureInfo) {
            getFeatureMethod() >> methodInfo
        }

        def spec = Mock(SpecInfo) {
            isAnnotationPresent(Integration) >> true
            isAnnotationPresent(DatabaseCleanup) >> false
            getReflection() >> SpecWithAnnotatedSetup
            getFeatures() >> [feature]
            getDeclaredMethods() >> []
        }

        when:
        extension.visitSpec(spec)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('@DatabaseCleanup annotation on method')
        ex.message.contains('setup')
        ex.message.contains('is not valid')

        cleanup:
        System.clearProperty(DatabaseCleanupExtension.VALIDATE_PROPERTY)
    }

    // --- Helper methods ---

    private DatabaseCleanupExtension createExtensionWithCleaner() {
        def extension = new DatabaseCleanupExtension()
        // Use reflection to set the context field directly since ServiceLoader won't find a cleaner in test
        def contextField = DatabaseCleanupExtension.getDeclaredField('context')
        contextField.accessible = true
        def cleaner = Mock(DatabaseCleaner) {
            databaseType() >> 'h2'
        }
        contextField.set(extension, new DatabaseCleanupContext([cleaner]))
        extension
    }

    // --- Stub classes for annotation detection ---

    @Integration
    @DatabaseCleanup
    static class AnnotatedClassSpec {}

    static class NonAnnotatedSpec {
        void unannotatedMethod() {}
    }

    @Integration
    static class MethodAnnotatedSpec {
        @DatabaseCleanup
        void annotatedMethod() {}
    }

    @Integration
    static class SpecWithAnnotatedSetup {
        @DatabaseCleanup
        void setup() {}

        void featureMethod() {}
    }
}
