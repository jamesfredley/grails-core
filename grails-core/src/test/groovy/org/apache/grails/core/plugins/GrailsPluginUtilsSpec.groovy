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
package org.apache.grails.core.plugins

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Test suite for GrailsPluginUtils utility methods
 */
class GrailsPluginUtilsSpec extends Specification {

    @Unroll
    def "isPluginVersionCompatible should check that plugin with grailsVersion=#pluginGrailsVersion is compatible with grails #grailsVersion"() {
        when:
        def compatible = PluginUtils.isPluginVersionCompatible(
            "1.0.0",  // pluginVersion
            pluginGrailsVersion,  // pluginSupportedVersion
            grailsVersion,  // grailsVersion
            "test-plugin"  // pluginDescription
        )

        then:
        compatible == expectedCompatible

        where:
        grailsVersion | pluginGrailsVersion        || expectedCompatible
        "1.0"         | "3.3.1 > *"                || false
        "2.5"         | "3.0.1"                    || false
        "3.0.0"       | "3.3.10 > *"               || false
        "3.3.10"      | "4.0.0 > *"                || false
        "4.0.1"       | "3.0.0.BUILD-SNAPSHOT > *" || true
        "4.0.1"       | "4.0.1"                    || true
        "4.0.1"       | "3.0.1"                    || false
        "4.0.1"       | "3.3.1 > *"                || true
        "4.0.1"       | "3.3.10 > *"               || true
    }

    def "isPluginVersionCompatible should handle null grailsVersion"() {
        when:
        def compatible = PluginUtils.isPluginVersionCompatible(
            "1.0.0",
            "3.3.10 > *",
            null,  // grailsVersion is null
            "test-plugin"
        )

        then:
        compatible == true
    }

    def "isPluginVersionCompatible should handle null pluginSupportedVersion"() {
        when:
        def compatible = PluginUtils.isPluginVersionCompatible(
            "1.0.0",
            null,  // pluginSupportedVersion is null
            "4.0.1",
            "test-plugin"
        )

        then:
        compatible == true
    }

    def "isPluginVersionCompatible should handle @ in pluginSupportedVersion"() {
        when:
        def compatible = PluginUtils.isPluginVersionCompatible(
            "1.0.0",
            "4.0.0@",  // contains @
            "3.0.0",
            "test-plugin"
        )

        then:
        compatible == true
    }

    // Tests for extractPluginMetadata method
    
    def "extractPluginMetadata extracts loadAfter, loadBefore, and dependsOn"() {
        when:
        def metadata = PluginUtils.extractPluginMetadata(PluginWithAllOrderingGrailsPlugin)

        then:
        metadata != null
        metadata.getName() == 'pluginWithAllOrdering'
        metadata.getLoadAfterNames() == ['alpha', 'beta'] as String[]
        metadata.getLoadBeforeNames() == ['gamma'] as String[]
        metadata.getDependsOnNames() == ['delta'] as String[]
    }

    def "extractPluginMetadata returns empty arrays for plugin with no ordering declarations"() {
        when:
        def metadata = PluginUtils.extractPluginMetadata(UtilsTestSimpleGrailsPlugin)

        then:
        metadata != null
        metadata.getName() == 'utilsTestSimple'
        metadata.getLoadAfterNames().length == 0
        metadata.getLoadBeforeNames().length == 0
        metadata.getDependsOnNames().length == 0
    }

    def "extractPluginMetadata returns null for class not ending in GrailsPlugin"() {
        expect:
        PluginUtils.extractPluginMetadata(String) == null
    }

    def "extractPluginMetadata returns null for null input"() {
        expect:
        PluginUtils.extractPluginMetadata(null) == null
    }

    def "extractPluginMetadata handles plugin that throws on instantiation"() {
        when:
        PluginUtils.extractPluginMetadata(FailingConstructorUtilsGrailsPlugin)

        then: "Throws IllegalStateException when plugin cannot be instantiated"
        thrown(IllegalStateException)
    }

    def "extractPluginMetadata extracts multiple dependsOn names"() {
        when:
        def metadata = PluginUtils.extractPluginMetadata(PluginWithMultipleDepsGrailsPlugin)

        then:
        metadata != null
        metadata.getName() == 'pluginWithMultipleDeps'
        metadata.getDependsOnNames() as Set == ['core', 'i18n'] as Set
    }

    def "readPluginConfiguration returns null when no plugin.yml or plugin.groovy exists"() {
        when:
        def resource = PluginUtils.readPluginConfiguration(UtilsTestSimpleGrailsPlugin)

        then: "No config file exists for this test fixture class"
        resource == null
    }

    def "getConfigurationResource returns resource that does not exist for non-existent path"() {
        when:
        def resource = PluginUtils.getConfigurationResource(
                UtilsTestSimpleGrailsPlugin, '/nonexistent.yml')

        then: "IOUtils constructs a URL but the resource does not exist on disk"
        resource == null || !resource.exists()
    }

    // Tests for additional public methods

    def "normalizePluginName converts hyphenated names to camelCase"() {
        expect:
        PluginUtils.normalizePluginName('my-plugin-name') == 'myPluginName'
        PluginUtils.normalizePluginName('simple') == 'simple'
        PluginUtils.normalizePluginName('already-camel') == 'alreadyCamel'
    }

    def "getLogicalPluginNameFromClassName extracts plugin name from class name"() {
        expect:
        PluginUtils.getLogicalPluginNameFromClassName('MyPluginGrailsPlugin') == 'myPlugin'
        PluginUtils.getLogicalPluginNameFromClassName('CoreGrailsPlugin') == 'core'
        PluginUtils.getLogicalPluginNameFromClassName('SimpleGrailsPlugin') == 'simple'
    }

    def "isGrailsPluginClassNamedCorrectly validates plugin naming convention"() {
        expect:
        PluginUtils.isGrailsPluginClassNamedCorrectly(PluginWithAllOrderingGrailsPlugin) == true
        PluginUtils.isGrailsPluginClassNamedCorrectly(UtilsTestSimpleGrailsPlugin) == true
        PluginUtils.isGrailsPluginClassNamedCorrectly(String) == false
        PluginUtils.isGrailsPluginClassNamedCorrectly(null) == false
    }

    def "isGrailsPluginLoadable returns false for null and DefaultGrailsPlugin"() {
        expect:
        PluginUtils.isGrailsPluginLoadable(PluginWithAllOrderingGrailsPlugin) == true
        PluginUtils.isGrailsPluginLoadable(null) == false
        // Note: Method returns true for String since it's not abstract and not DefaultGrailsPlugin.class
    }

    def "supportsValueInIncludeExcludeMap returns true for empty map"() {
        when:
        def emptyMap = [:]

        then:
        PluginUtils.supportsValueInIncludeExcludeMap(emptyMap, 'anything') == true
    }

    def "supportsValueInIncludeExcludeMap checks includes/excludes"() {
        when:
        def mapWithIncludes = ['includes': ['dev', 'test'] as Set]

        then:
        PluginUtils.supportsValueInIncludeExcludeMap(mapWithIncludes, 'dev') == true
        PluginUtils.supportsValueInIncludeExcludeMap(mapWithIncludes, 'prod') == false
    }

    def "supportsValueInIncludeExcludeMap checks excludes"() {
        when:
        def mapWithExcludes = ['excludes': ['prod'] as Set]

        then:
        PluginUtils.supportsValueInIncludeExcludeMap(mapWithExcludes, 'dev') == true
        PluginUtils.supportsValueInIncludeExcludeMap(mapWithExcludes, 'prod') == false
    }

    def "scanPluginDescriptors returns empty list when no descriptors found"() {
        when:
        def emptyClassLoader = new URLClassLoader([] as URL[], (ClassLoader) null)
        def classNames = PluginUtils.scanPluginDescriptors(emptyClassLoader)

        then:
        classNames.isEmpty()
    }

    def "scanPluginDescriptorResources returns empty list when no descriptors found"() {
        when:
        def emptyClassLoader = new URLClassLoader([] as URL[], (ClassLoader) null)
        def descriptors = PluginUtils.scanPluginDescriptorResources(emptyClassLoader)

        then:
        descriptors.isEmpty()
    }

    def "getLogicalPluginName derives plugin name from plugin class"() {
        expect:
        PluginUtils.getLogicalPluginName(PluginWithAllOrderingGrailsPlugin) == 'pluginWithAllOrdering'
        PluginUtils.getLogicalPluginName(UtilsTestSimpleGrailsPlugin) == 'utilsTestSimple'
        PluginUtils.getLogicalPluginName(PluginWithMultipleDepsGrailsPlugin) == 'pluginWithMultipleDeps'
    }

    def "evaluateIncludeExcludeProperty parses include/exclude map structure"() {
        given:
        def plugin = new GroovyObject() {
            def environments = [
                'includes': ['dev', 'test'],
                'excludes': ['prod']
            ]
        }

        when:
        def result = PluginUtils.evaluateIncludeExcludeProperty(
            plugin,
            'environments',
            { obj -> obj }  // identity converter
        )

        then:
        result.containsKey('includes')
        result.containsKey('excludes')
        result['includes'] == ['dev', 'test'] as Set
        result['excludes'] == ['prod'] as Set
    }

    def "evaluateIncludeExcludeProperty handles string values"() {
        given:
        def plugin = new GroovyObject() {
            def scope = 'development'
        }

        when:
        def result = PluginUtils.evaluateIncludeExcludeProperty(
            plugin,
            'scope',
            { obj -> obj }  // identity converter
        )

        then:
        result.containsKey('includes')
        result['includes'] == ['development'] as Set
    }

    def "evaluateIncludeExcludeProperty handles list values"() {
        given:
        def plugin = new GroovyObject() {
            def scopes = ['dev', 'test']
        }

        when:
        def result = PluginUtils.evaluateIncludeExcludeProperty(
            plugin,
            'scopes',
            { obj -> obj }  // identity converter
        )

        then:
        result.containsKey('includes')
        result['includes'] == ['dev', 'test'] as Set
    }
}

// Test fixture plugin classes for GrailsPluginUtilsSpec

class UtilsTestSimpleGrailsPlugin {
    def version = '1.0'
}

class PluginWithAllOrderingGrailsPlugin {
    def version = '1.0'
    def loadAfter = ['alpha', 'beta']
    def loadBefore = ['gamma']
    def dependsOn = [delta: '1.0']
}

class FailingConstructorUtilsGrailsPlugin {
    def version = '1.0'

    FailingConstructorUtilsGrailsPlugin() {
        throw new RuntimeException("Intentional failure for testing")
    }
}

class PluginWithMultipleDepsGrailsPlugin {
    def version = '1.0'
    def dependsOn = [core: '1.0', i18n: '1.0']
}
