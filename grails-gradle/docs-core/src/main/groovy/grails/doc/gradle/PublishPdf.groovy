/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.doc.gradle


import grails.doc.PdfPublisher
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

/**
 * Gradle task for generating a gdoc-based PDF user guide. Assumes the
 * single page HTML user guide has already been created in the default
 * location.
 */
abstract class PublishPdf extends DefaultTask {
    @Input String pdfName = "single.pdf"
    @Input String language = ""
    @OutputDirectory
    final abstract Property<Directory> outputDirectory

    @Inject
    PublishPdf(ObjectFactory objects) {
        outputDirectory = objects.directoryProperty().convention(project.layout.buildDirectory.dir("pdf"))
    }

    @TaskAction
    def publish() {
        File baseOutputDir = outputDirectory.get().asFile
        File i18nOutputDir = new File(baseOutputDir, language ?: "")
        try {
            PdfPublisher.publishPdfFromHtml(i18nOutputDir, "guide/single.html", pdfName)
        }
        catch (Exception ex) {
            ex.printStackTrace()
        }
    }
}
