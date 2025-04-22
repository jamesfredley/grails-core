package grails.doc

import groovy.transform.CompileStatic
import org.w3c.dom.Document

@CompileStatic
class PdfPublisher {

    static void publishPdfFromHtml(File outputDir, File inputFile, String pdfName) {
        PdfBuilder pdfBuilder = new PdfBuilder()
        String xml = pdfBuilder.createXml(inputFile, outputDir.absolutePath)
        Document doc = pdfBuilder.createDocument(xml)
        File outputFile = new File(outputDir, pdfName)
        pdfBuilder.createPdfWithDocument(doc, outputFile, inputFile)
    }
}
