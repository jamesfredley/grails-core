package org.apache.grails.buildsrc

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project

import static org.apache.grails.buildsrc.GradleUtils.findRootGrailsCoreDir
import static org.apache.grails.buildsrc.GradleUtils.lookupPropertyByType

/**
 * Gradle can't share properties across buildSrc or composite projects. This plugin ensures that properties not defined
 * in this project, but are in the root grails-core project, are accessible in this project. This plugin must be applied
 * prior to the access of any property for it to work properly
 */
@CompileStatic
class SharedPropertyPlugin  implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def ext = project.extensions.getExtraProperties()
        ext.set('grailsVersion', lookupPropertyByType(project, 'projectVersion', String))

        def rootGrailsCoreDir = findRootGrailsCoreDir(project)
        if(project.layout.projectDirectory.asFile.name == 'build-src') {
            // Load the subproject properties first
            def parentProject = project.layout.projectDirectory.dir('..')
            if(parentProject.asFile.absolutePath != rootGrailsCoreDir.asFile.absolutePath) {
                parentProject.file('gradle.properties').asFile.withInputStream {
                    Properties projectProperties = new Properties()
                    projectProperties.load(it)

                    for (String key : projectProperties.stringPropertyNames()) {
                        ext.set(key, projectProperties.getProperty(key))
                    }
                }
            }
        }

        rootGrailsCoreDir.file('gradle.properties').asFile.withInputStream {
            Properties rootProperties = new Properties()
            rootProperties.load(it)

            for (String key : rootProperties.stringPropertyNames()) {
                if(!ext.has(key)) {
                    ext.set(key, rootProperties.getProperty(key))
                }
            }
        }
    }
}
