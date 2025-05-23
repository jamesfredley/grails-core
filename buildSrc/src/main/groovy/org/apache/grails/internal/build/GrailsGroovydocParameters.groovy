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

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.api.tasks.javadoc.GroovydocAccess
import org.gradle.internal.enterprise.test.FileProperty
import org.gradle.workers.WorkParameters

interface GrailsGroovydocParameters extends WorkParameters {
    Property<String> getIncludeAuthor()
    Property<String> getCharset()
    Property<String> getDocTitle()
    Property<String> getWindowTitle()
    Property<String> getFileEncoding()
    Property<String> getFooter()
    Property<String> getHeader()
    Property<Boolean> getNoMainForScripts()
    Property<Boolean> getIncludeScripts()
    Property<Boolean> getNoTimestamp()
    Property<Boolean> getNoVersionTimestamp()
    Property<String> getJavaVersion()
    Property<GroovydocAccess> getAccess()
    FileProperty getOverview()
    ConfigurableFileCollection getSource()
    DirectoryProperty getDestinationDirectory()
    Property<Boolean> getIncludeUsagePages() // use flag
    SetProperty<Groovydoc.Link> getLinks();
    RegularFileProperty getTmpDir();
}
