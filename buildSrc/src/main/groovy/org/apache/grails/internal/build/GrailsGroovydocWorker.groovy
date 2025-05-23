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

import org.apache.groovy.util.SystemUtil
import org.codehaus.groovy.tools.groovydoc.ClasspathResourceManager
import org.codehaus.groovy.tools.groovydoc.GroovyDocTool
import org.codehaus.groovy.tools.groovydoc.LinkArgument
import org.codehaus.groovy.tools.groovydoc.gstringTemplates.GroovyDocTemplateInfo
import org.gradle.api.tasks.javadoc.Groovydoc
import org.gradle.workers.WorkAction

abstract class GrailsGroovydocWorker implements WorkAction<GrailsGroovydocParameters> {
    @Override
    void execute() {
        GrailsGroovydocParameters parameters = getParameters()

        List<String> sourcePaths = parameters.source.getFiles().collect{ it.absolutePath }

        List<String> packagesToDoc = new ArrayList<>();
        Properties properties = new Properties();
        properties.setProperty("windowTitle", windowTitle);
        properties.setProperty("docTitle", docTitle);
        properties.setProperty("footer", footer);
        properties.setProperty("header", header);
        checkScopeProperties(properties);
        properties.setProperty("publicScope", publicScope.toString());
        properties.setProperty("protectedScope", protectedScope.toString());
        properties.setProperty("packageScope", packageScope.toString());
        properties.setProperty("privateScope", privateScope.toString());
        properties.setProperty("author", author.toString());
        properties.setProperty("processScripts", processScripts.toString());
        properties.setProperty("includeMainForScripts", includeMainForScripts.toString());
        properties.setProperty("overviewFile", overviewFile != null ? overviewFile.getAbsolutePath() : "");
        properties.setProperty("charset", charset != null ? charset : "");
        properties.setProperty("fileEncoding", fileEncoding != null ? fileEncoding : "");
        properties.setProperty("timestamp", Boolean.valueOf(!noTimestamp).toString());
        properties.setProperty("versionStamp", Boolean.valueOf(!noVersionStamp).toString());
        String phaseOverride = SystemUtil.getSystemPropertySafe("groovydoc.phase.override");
        if (phaseOverride != null) properties.put("phaseOverride", phaseOverride);

        if (sourcePath != null) {
            sourceDirs.addExisting(sourcePath);
        }

        def links = parameters.links.get().collect{ link ->
            LinkArgument arg = new LinkArgument()
            arg.setHref(link.url)
            arg.setPackages(link.packages.join(","))
            arg
        }

        GroovyDocTool htmlTool = new GroovyDocTool(
                new ClasspathResourceManager(), // we're gonna get the default templates out of the dist jar file
                sourcePaths as String[],
                GroovyDocTemplateInfo.DEFAULT_DOC_TEMPLATES,
                GroovyDocTemplateInfo.DEFAULT_PACKAGE_TEMPLATES,
                GroovyDocTemplateInfo.DEFAULT_CLASS_TEMPLATES,
                links,
                parameters.javaVersion.getOrNull(),
                properties
        );
    }
}
