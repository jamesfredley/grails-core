package org.apache.grails.buildsrc

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

import groovy.transform.CompileStatic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.GroovyCompile
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions

import static org.apache.grails.buildsrc.GradleUtils.lookupPropertyByType

@CompileStatic
class CompilePlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def initialized = new AtomicBoolean(false)
        project.plugins.withId('java') { // java (applied when groovy is applied) or java-library
            if (initialized.compareAndSet(false, true)) {
                configureCompile(project)
            }
        }
    }

    private static void configureCompile(Project project) {
        configureJavaVersion(project)
        configureJars(project)
        configureCompiler(project)
        configureReproducible(project)
    }

    private static void configureJavaVersion(Project project) {
        project.tasks.withType(JavaCompile).configureEach {
            it.options.release.set(lookupPropertyByType(project, 'javaVersion', Integer))
        }
    }

    private static void configureJars(Project project) {
        project.extensions.configure(JavaPluginExtension) {
            // Explicit `it` is required here
            it.withJavadocJar()
            it.withSourcesJar()
        }

        // TODO: Causes the imports autoconfiguration file to be duplicated in grails-core b/c main gets copied twice
        // TODO: this was for grails-gradle specifically in the past, we need to figure out which additional source sets are included
//        project.tasks.named('sourcesJar', Jar).configure { Jar jar ->
//            SourceSetContainer sourceSets = project.extensions.getByType(JavaPluginExtension).sourceSets
//
//            // don't only include main, but any source set
//            jar.from(sourceSets.collect { it.allSource })
//            jar.inputs.files(sourceSets.collect { it.allSource })
//        }

        // Grails determines the grails version via the META-INF/MANIFEST.MF file
        // Note: we exclude attributes such as Built-By, Build-Jdk, Created-By to ensure the build is reproducible.
        project.tasks.withType(Jar).configureEach { Jar jar ->
            if (lookupPropertyByType(project, 'skipJavaComponent', Boolean)) {
                jar.enabled = false
                return
            }

            jar.manifest.attributes(
                    'Implementation-Title': 'Apache Grails',
                    'Implementation-Version': lookupPropertyByType(project, 'grailsVersion', String),
                    'Implementation-Vendor': 'grails.apache.org'
            )
            // TODO: forge used to be include, grails used to exclude
            jar.duplicatesStrategy = DuplicatesStrategy.FAIL
        }
    }

    private static void configureCompiler(Project project) {
        project.tasks.withType(JavaCompile).configureEach {
            // Preserve method parameter names in Groovy/Java classes for IDE parameter hints & bean reflection metadata.
            it.options.compilerArgs.add('-parameters')
            it.options.encoding = StandardCharsets.UTF_8.name()
            // encoding needs to be the same since it's different across platforms
            it.options.fork = true
            it.options.forkOptions.jvmArgs = ['-Xms128M', '-Xmx2G']
        }

        project.plugins.withId('groovy') {
            project.tasks.withType(GroovyCompile).configureEach {
                it.groovyOptions.encoding = StandardCharsets.UTF_8.name()
                // encoding needs to be the same since it's different across platforms
                // Preserve method parameter names in Groovy/Java classes for IDE parameter hints & bean reflection metadata.
                it.groovyOptions.parameters = true
                it.options.encoding = StandardCharsets.UTF_8.name()
                // encoding needs to be the same since it's different across platforms
                it.options.fork = true
                it.options.forkOptions.jvmArgs = ['-Xms128M', '-Xmx2G']
            }
        }
    }

    private static void configureReproducible(Project project) {
        project.tasks.withType(Javadoc).configureEach { Javadoc it ->
            def options = it.options as StandardJavadocDocletOptions
            options.noTimestamp = true
            options.bottom = "Generated ${lookupPropertyByType(project, 'formattedBuildDate', String)} (UTC)"
        }

        // Any jar, zip, or archive should be reproducible
        // No longer needed after https://github.com/gradle/gradle/issues/30871
        project.tasks.withType(AbstractArchiveTask).configureEach {
            it.preserveFileTimestamps = false // to prevent timestamp mismatches
            it.reproducibleFileOrder = true // to keep the same ordering
            // to avoid platform specific defaults, set the permissions consistently
            it.filePermissions { permissions ->
                permissions.unix(0644)
            }
            it.dirPermissions { permissions ->
                permissions.unix(0755)
            }
        }
    }
}
