package org.apache.grails.gradle.tasks.bom

import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import groovy.transform.ToString

@CompileStatic
@MapConstructor(includes = ['groupId', 'artifactId', 'version', 'versionProperty', 'source'], includeSuperProperties = true)
@ToString(includes = ['groupId', 'artifactId', 'version', 'versionProperty', 'source'], includeSuperProperties = true)
class ExtractedDependencyConstraint extends CoordinateVersionHolder {
    String versionProperty
    String source

    ExtractedDependencyConstraint(String coordinates) {
        coordinates.split(':').with { String[] parts ->
            groupId = parts[0]
            artifactId = parts[1]
            version = parts[2]
        }
    }

    String getVersionProperty() {
        versionProperty == '${project.version}' ? '' : versionProperty
    }
}