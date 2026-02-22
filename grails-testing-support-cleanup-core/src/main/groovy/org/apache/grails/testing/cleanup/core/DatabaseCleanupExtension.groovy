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

import java.lang.reflect.Method

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import grails.testing.mixin.integration.Integration

import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.FeatureInfo
import org.spockframework.runtime.model.SpecInfo

/**
 * Spock global extension that detects the {@link DatabaseCleanup} annotation on test classes
 * and methods, and registers the {@link DatabaseCleanupInterceptor} to perform database
 * cleanup after annotated tests.
 *
 * <p>This extension requires the {@link Integration @Integration} annotation to be present
 * on the spec class. If a spec uses {@code @DatabaseCleanup} (at the class or method level)
 * without {@code @Integration}, an {@link IllegalStateException} is thrown, since
 * {@code @DatabaseCleanup} depends on the Spring application context provided by the
 * integration test infrastructure.</p>
 *
 * <p>The extension uses the {@link ServiceLoader} mechanism to discover all available
 * {@link DatabaseCleaner} implementations at startup. It validates that each implementation
 * declares a unique {@link DatabaseCleaner#databaseType() databaseType}; if duplicates are
 * found, an error is thrown immediately.</p>
 *
 * <p>At runtime, when cleanup is triggered for a datasource, the framework verifies that
 * a matching cleaner exists for that datasource's database type. If no matching cleaner
 * is found, an error is raised.</p>
 *
 * <p>By default, validation of misplaced {@code @DatabaseCleanup} annotations on non-feature
 * methods (e.g., {@code setup()}, {@code cleanup()}) is disabled for performance reasons.
 * Set the system property {@code grails.testing.cleanup.validate=true} to opt into this check:</p>
 * <pre>-Dgrails.testing.cleanup.validate=true</pre>
 */
@Slf4j
@CompileStatic
class DatabaseCleanupExtension implements IGlobalExtension {

    /**
     * System property that enables validation of {@code @DatabaseCleanup} placement on
     * non-feature methods. Set to {@code "true"} to enable:
     * {@code -Dgrails.testing.cleanup.validate=true}
     */
    static final String VALIDATE_PROPERTY = 'grails.testing.cleanup.validate'

    private DatabaseCleanupContext context

    /**
     * Returns {@code true} if the {@code grails.testing.cleanup.validate} system property
     * is set to {@code "true"}.
     */
    static boolean isValidateEnabled() {
        Boolean.getBoolean(VALIDATE_PROPERTY)
    }

    @Override
    void start() {
        ServiceLoader<DatabaseCleaner> cleanerLoader = ServiceLoader.load(DatabaseCleaner)
        List<DatabaseCleaner> cleaners = cleanerLoader.toList()

        if (cleaners.isEmpty()) {
            log.debug('No DatabaseCleaner implementations found on classpath')
            return
        }

        // Validate uniqueness of databaseType across all cleaners
        Map<String, DatabaseCleaner> typeMap = [:]
        for (DatabaseCleaner cleaner : cleaners) {
            String type = cleaner.databaseType()
            if (!type || type.trim().isEmpty()) {
                throw new IllegalStateException(
                    "DatabaseCleaner implementation ${cleaner.class.name} returned a null or empty databaseType()" as String)
            }

            DatabaseCleaner existing = typeMap.get(type)
            if (existing) {
                throw new IllegalStateException(
                    "Duplicate databaseType '${type}' declared by both ${existing.class.name} and ${cleaner.class.name}. Each DatabaseCleaner must declare a unique databaseType." as String)
            }

            typeMap.put(type, cleaner)
            log.debug('Discovered DatabaseCleaner implementation: {} (type: {})', cleaner.class.name, type)
        }

        context = new DatabaseCleanupContext(cleaners)
    }

    @Override
    void visitSpec(SpecInfo spec) {
        if (!context) {
            return
        }

        // @DatabaseCleanup requires @Integration since the cleanup framework depends on the
        // Spring application context provided by the integration test infrastructure
        if (!spec.isAnnotationPresent(Integration)) {
            if (hasDatabaseCleanupAnnotation(spec)) {
                throw new IllegalStateException(
                    "@DatabaseCleanup requires @Integration on spec '${spec.name}'. The database cleanup framework depends on the Spring application context provided by the integration test infrastructure." as String)
            }
            return
        }

        boolean classAnnotated = spec.isAnnotationPresent(DatabaseCleanup)

        if (classAnnotated) {
            DatabaseCleanup annotation = spec.getAnnotation(DatabaseCleanup)
            DatasourceCleanupMapping mapping = DatasourceCleanupMapping.parse(annotation.value())
            ApplicationContextResolver resolver = createResolver(annotation.resolver())
            DatabaseCleanupInterceptor interceptor = new DatabaseCleanupInterceptor(context, true, mapping, resolver)
            spec.addSetupInterceptor(interceptor)
            spec.addCleanupInterceptor(interceptor)
            log.debug('Registered DatabaseCleanupInterceptor for spec: {} (class-level)', spec.name)
            return
        }

        // Validate that @DatabaseCleanup is not placed on non-feature methods (setup, cleanup, etc.)
        // This validation is opt-in via -Dgrails.testing.cleanup.validate=true for performance reasons
        if (validateEnabled) {
            validateNoAnnotationOnNonFeatureMethods(spec)
        }

        // Check for method-level annotations on feature methods
        boolean hasMethodAnnotation = false
        for (FeatureInfo feature : spec.features) {
            if (feature.featureMethod.isAnnotationPresent(DatabaseCleanup)) {
                hasMethodAnnotation = true
                break
            }
        }

        if (hasMethodAnnotation) {
            // For method-level, pass a clean-all mapping as default; the interceptor reads
            // each method's own annotation at runtime.
            // Use the default resolver; the interceptor will read the method-level resolver at runtime.
            DatasourceCleanupMapping defaultMapping = DatasourceCleanupMapping.parse(new String[0])
            ApplicationContextResolver defaultResolver = new DefaultApplicationContextResolver()
            DatabaseCleanupInterceptor interceptor = new DatabaseCleanupInterceptor(context, false, defaultMapping, defaultResolver)
            spec.addSetupInterceptor(interceptor)
            spec.addCleanupInterceptor(interceptor)
            log.debug('Registered DatabaseCleanupInterceptor for spec: {} (method-level)', spec.name)
        }
    }

    /**
     * Creates an instance of the specified {@link ApplicationContextResolver} class.
     *
     * @param resolverClass the resolver class to instantiate
     * @return a new resolver instance
     */
    private static ApplicationContextResolver createResolver(Class<? extends ApplicationContextResolver> resolverClass) {
        try {
            resolverClass.getDeclaredConstructor().newInstance()
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Failed to instantiate ApplicationContextResolver: ${resolverClass.name}. Ensure it has a no-arg constructor." as String, e)
        }
    }

    /**
     * Validates that {@link DatabaseCleanup} is not placed on non-feature methods such as
     * setup(), cleanup(), setupSpec(), or cleanupSpec(). The annotation is only valid on
     * Spock feature methods (test methods) or at the class level.
     *
     * <p>This validation is opt-in and only runs when the system property
     * {@code grails.testing.cleanup.validate} is set to {@code "true"}, since iterating
     * over all declared methods via reflection has a performance cost.</p>
     *
     * @param spec the spec to validate
     * @throws IllegalStateException if @DatabaseCleanup is found on a non-feature method
     */
    private static void validateNoAnnotationOnNonFeatureMethods(SpecInfo spec) {
        // Collect all feature method names for comparison
        Set<String> featureMethodNames = [] as Set
        for (FeatureInfo feature : spec.features) {
            featureMethodNames.add(feature.featureMethod.name)
        }

        // Check all declared methods in the spec class for misplaced annotations
        // This necessarily uses spec.reflection since we need to scan raw Java methods
        // that are not exposed as Spock features
        for (Method method : spec.reflection.declaredMethods) {
            if (method.isAnnotationPresent(DatabaseCleanup) && !featureMethodNames.contains(method.name)) {
                throw new IllegalStateException(
                    "@DatabaseCleanup annotation on method '${method.name}' in ${spec.reflection.name} is not valid. @DatabaseCleanup can only be applied to Spock feature methods (test methods) or at the class level. It cannot be applied to setup(), cleanup(), setupSpec(), or cleanupSpec() methods." as String)
            }
        }
    }

    /**
     * Checks whether the spec has any {@link DatabaseCleanup} annotation, either at the
     * class level or on any feature method.
     */
    private static boolean hasDatabaseCleanupAnnotation(SpecInfo spec) {
        if (spec.isAnnotationPresent(DatabaseCleanup)) {
            return true
        }
        for (FeatureInfo feature : spec.features) {
            if (feature.featureMethod.isAnnotationPresent(DatabaseCleanup)) {
                return true
            }
        }
        false
    }
}
