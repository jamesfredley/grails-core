package org.apache.grails.buildsrc

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.plugins.ExtraPropertiesExtension

import static org.apache.grails.buildsrc.GradleUtils.findRootGrailsCoreDir

/**
 * Gradle can't share properties across buildSrc or composite projects. This plugin ensures that properties not defined
 * in this project, but are in the root grails-core project, are accessible in this project. This plugin must be applied
 * prior to the access of any property for it to work properly
 */
@CompileStatic
class SharedPropertyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def ext = project.extensions.getExtraProperties()

        def rootGrailsCoreDir = findRootGrailsCoreDir(project)
        populateParentProperties(project.layout.projectDirectory, rootGrailsCoreDir, ext, project)
    }

    void populateParentProperties(Directory projectDirectory, Directory rootDirectory, ExtraPropertiesExtension ext, Project project) {
        if (!rootDirectory) {
            throw new IllegalStateException('Could not locate the root directory to populate up to')
        }

        if (projectDirectory.file('gradle.properties').asFile.exists()) {
            def propertyPath = rootDirectory.asFile.relativePath(projectDirectory.asFile)
            project.logger.info('Using properties from grails-core/{}gradle.properties', propertyPath ? "${propertyPath}/" : '')
            projectDirectory.file('gradle.properties').asFile.withInputStream {
                Properties rootProperties = new Properties()
                rootProperties.load(it)

                for (String key : rootProperties.stringPropertyNames()) {
                    if (!ext.has(key)) {
                        ext.set(key, rootProperties.getProperty(key))
                    }
                }

                if (rootProperties.containsKey('projectVersion')) {
                    ext.set('grailsVersion', rootProperties.getProperty('projectVersion'))
                }
            }
        }

        if (projectDirectory.asFile.absolutePath != rootDirectory.asFile.absolutePath) {
            populateParentProperties(projectDirectory.dir('..'), rootDirectory, ext, project)
        }
    }
}
