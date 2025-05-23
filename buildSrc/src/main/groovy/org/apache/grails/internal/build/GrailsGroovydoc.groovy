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

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.UncheckedIOException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.internal.tasks.GroovydocAntAction
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.javadoc.GroovydocAccess
import org.gradle.internal.enterprise.test.FileProperty
import org.gradle.process.JavaForkOptions
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

/**
 * Uses process isolation to reduce memory footprint in Groovydoc
 */
@CacheableTask
abstract class GrailsGroovydoc extends DefaultTask {
    @Input
    final Property<String> author

    @Input
    final Property<String> charset

    @Input
    final Property<Boolean> debugOutput

    @Input
    final Property<String> docTitle

    @Input
    final Property<String> fileEncoding

    @Input
    final Property<String> footer

    @Input
    final Property<String> header

    @Input
    final Property<Boolean> noMainForScripts

    @Input
    final Property<Boolean> noScripts

    @Input
    final Property<Boolean> noTimestamp

    @Input
    final Property<Boolean> noVersionTimestamp

    @Input
    final Property<GroovydocAccess> access

    @Nested
    JavaForkOptions forkOptions

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    final FileProperty overview

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    final ConfigurableFileCollection groovyClasspath // where to locate the groovydoc library

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    final ConfigurableFileCollection source // files to generate groovydoc for

    @OutputDirectory
    final DirectoryProperty destinationDirectory

    private final ObjectFactory objects
    private final WorkerExecutor workerExecutor

    @Inject
    GrailsGroovydoc(ObjectFactory objects, WorkerExecutor workerExecutor) {
        this.objects = objects
        this.workerExecutor = workerExecutor

        author = objects.property(String)
        charset = objects.property(String)
        debugOutput = objects.property(Boolean)
        docTitle = objects.property(String)
        fileEncoding = objects.property(String)
        footer = objects.property(String)
        header = objects.property(String)
        noMainForScripts = objects.property(Boolean)
        noScripts = objects.property(Boolean)
        noTimestamp = objects.property(Boolean)
        noVersionTimestamp = objects.property(Boolean)
        access = objects.property(GroovydocAccess)
        groovyClasspath = objects.fileCollection()
        source = objects.fileCollection()
        destinationDirectory = objects.directoryProperty()
        forkOptions = objects.newInstance(JavaForkOptions)
    }

    @TaskAction
    protected void generate() {
        if (this.getGroovyClasspath().getFiles().isEmpty()) {
            throw new InvalidUserDataException("You must assign a Groovy library to the groovy configuration!")
        }

        workerExecutor.processIsolation {
             it.forkOptions.jvmArgs = forkOptions.jvmArgs
             it.forkOptions.debug = forkOptions.debug
             it.forkOptions.executable = forkOptions.executable
        }.submit(GrailsGroovydocWorker) {
            params()
        }


        File destinationDir = this.getDestinationDir()

        try {
            this.getDeleter().ensureEmptyDirectory(destinationDir)
        } catch (IOException ex) {
            throw new UncheckedIOException(ex)
        }

        FileSystemOperations fsOperations = (FileSystemOperations) this.getServices().get(FileSystemOperations.class)
        File tmpDir = this.getTemporaryDir()
        fsOperations.delete((spec) -> spec.delete(new Object[]{tmpDir}))
        fsOperations.copy((spec) -> spec.from(new Object[]{this.getSource()}).into(tmpDir))

        List cp = getClasspath().collect { it } + getGroovyClasspath().collect { it }
        this.getWorkerExecutor().classLoaderIsolation().submit(GroovydocAntAction.class, (parameters) -> {
            parameters.getAntLibraryClasspath().from(new Object[]{cp})
            parameters.getSource().convention(this.getSource())
            parameters.getDestinationDirectory().fileValue(destinationDir)
            parameters.getUse().convention(this.isUse())
            parameters.getNoTimestamp().convention(this.isNoTimestamp())
            parameters.getNoVersionStamp().convention(this.isNoVersionStamp())
            parameters.getWindowTitle().convention(this.getWindowTitle())
            parameters.getDocTitle().convention(this.getDocTitle())
            parameters.getHeader().convention(this.getHeader())
            parameters.getFooter().convention(this.getFooter())
            TextResource overview = this.getOverviewText()
            parameters.getOverview().convention(overview != null ? overview.asFile().getAbsolutePath() : null)
            parameters.getAccess().convention(this.getAccess())
            parameters.getLinks().convention(this.getLinks())
            parameters.getTmpDir().fileValue(this.getTemporaryDir())
            parameters.getIncludeAuthor().convention(this.getIncludeAuthor())
            parameters.getProcessScripts().convention(this.getProcessScripts())
            parameters.getIncludeMainForScripts().convention(this.getIncludeMainForScripts())
        })
    }
}
