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

package grails.doc.git


import groovy.transform.CompileStatic
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CompileStatic
@CacheableTask
abstract class FetchTagsTask extends JavaExec {

    @OutputFile
    abstract RegularFileProperty getOutputFile() // Declares output
    /**
     * List all tags in the local Git repository.
     *
     * @param repoSlug The slug of the repository. e.g. apache/grails-core
     * @return The list of tags in the repository
     */
    @TaskAction
    private List<String> listRepoTags() {
        File repoRoot = project.rootProject.projectDir
        def command = ["git", "-C", repoRoot.absolutePath, "tag", "-l", "--sort=-creatordate"]

        def process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()

        process.waitFor()

        if (process.exitValue() != 0) {
            throw new GradleException("Failed executing Git command to fetch version tags: ${process.text}")
        }

        def tags = process.text.readLines()*.trim()

    }
}
