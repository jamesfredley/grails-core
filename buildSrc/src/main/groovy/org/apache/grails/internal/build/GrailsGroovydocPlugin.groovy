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

package org.apache.grails.internal.build

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.internal.JvmFeatureInternal
import org.gradle.api.tasks.GroovySourceDirectorySet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.javadoc.GroovydocAccess

import javax.inject.Inject

@CompileStatic
abstract class GrailsGroovydocPlugin implements Plugin<Project> {

    private ObjectFactory objects

    @Inject
    GrailsGroovydocPlugin(ObjectFactory objects) {
        this.objects = objects
    }

    @CompileDynamic
    String resolveProjectVersion(String artifact, Project project) {
        String version = project.configurations.runtimeClasspath
                .resolvedConfiguration
                .resolvedArtifacts
                .find {
                    it.moduleVersion.id.name == artifact
                }?.moduleVersion?.id?.version
        if (!version) {
            return null
        }
    }

    @Override
    void apply(Project project) {
        NamedDomainObjectProvider<Configuration> documentation = project.configurations.register('documentation') { Configuration doc ->
            doc.canBeConsumed = false
            doc.canBeResolved = true
            doc.attributes {
                it.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category, Category.LIBRARY))
                it.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling, Bundling.EXTERNAL))
                it.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage, Usage.JAVA_RUNTIME))
            }
        }

        project.dependencies.add('documentation', project.dependencies.platform(project.rootProject.project(':grails-bom')))
        project.dependencies.add('documentation', 'org.fusesource.jansi:jansi')
        project.dependencies.add('documentation', 'jline:jline')
        project.dependencies.add('documentation', 'com.github.javaparser:javaparser-core')
        project.dependencies.add('documentation', 'org.apache.groovy:groovy')
        project.dependencies.add('documentation', 'org.apache.groovy:groovy-groovydoc')
        project.dependencies.add('documentation', 'org.apache.groovy:groovy-ant')
        project.dependencies.add('documentation', 'org.apache.groovy:groovy-docgenerator')
        project.dependencies.add('documentation', 'org.apache.groovy:groovy-templates')

        TaskProvider<GrailsGroovydoc> grailsGroovydoc = project.tasks.register('grailsGroovydoc', GrailsGroovydoc)
//        grailsGroovydoc.configure { GrailsGroovydoc gdoc ->
//            gdoc.exclude('META-INF/**', '*yml', '*properties', '*xml', '**/Application.groovy', '**/Bootstrap.groovy', '**/resources.groovy')
//            gdoc.description = 'Generate Groovydoc API documentation for the main source code in an isolated process.'
//            gdoc.group = 'documentation'
//
//            JvmFeatureInternal mainFeature = JavaPluginHelper.getJavaComponent(project).mainFeature
//            gdoc.classpath = mainFeature.sourceSet.output.plus(mainFeature.sourceSet.compileClasspath)
//            gdoc.source = (SourceDirectorySet) mainFeature.sourceSet.extensions.getByType(GroovySourceDirectorySet)
//
//            gdoc.groovyClasspath = project.files(documentation)
//            gdoc.windowTitle = "${project.findProperty('pomArtifactId') ?: project.name} - ${project.findProperty('projectVersion')}"
//            gdoc.docTitle = "${project.findProperty('pomArtifactId') ?: project.name} - ${project.findProperty('projectVersion')}"
//            gdoc.access.set(GroovydocAccess.PROTECTED)
//            gdoc.includeAuthor.set(true)
//            gdoc.includeMainForScripts.set(false)
//            gdoc.processScripts.set(false)
//            gdoc.noTimestamp = true
//
//            gdoc.doFirst {
//                def gebVersion = resolveProjectVersion('geb-spock', project)
//                if(gebVersion) {
//                    gdoc.link("https://www.gebish.org/manual/${gebVersion}/api/", 'geb.')
//                }
//
//                def testContainersVersion = resolveProjectVersion('testcontainers', project)
//                if(testContainersVersion) {
//                    gdoc.link("https://javadoc.io/doc/org.testcontainers/testcontainers/${testContainersVersion}/", 'org.testcontainers.')
//                }
//
//                def springVersion = resolveProjectVersion('spring-core', project)
//                if(springVersion) {
//                    gdoc.link("https://docs.spring.io/spring-framework/docs/${springVersion}/javadoc-api/", 'org.springframework.core.')
//                }
//
//                def springBootVersion = resolveProjectVersion('spring-boot', project)
//                if(springBootVersion) {
//                    gdoc.link("https://docs.spring.io/spring-boot/docs/${springBootVersion}/api/", 'org.springframework.boot.')
//                }
//            }
//        }

        project.tasks.named('groovydoc').configure {groovydoc ->
            groovydoc.enabled = false
        }
    }
}
