package de.kugidev.pdf.pdfa;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Fügt Eingabedateien (JPG, PNG, PDF) zu einem gemeinsamen {@link PDDocument} zusammen.
 * Bilder werden auf eine A4-Seite zentriert, PDF-Seiten unverändert importiert.
 */
@Slf4j
class SourceDocumentMerger {

    private final PDFMergerUtility pdfMerger = new PDFMergerUtility();

    PDDocument merge(List<File> inputFiles) throws IOException {
        PDDocument merged = new PDDocument();
        try {
            for (File file : inputFiles) {
                append(merged, file);
            }
            return merged;
        } catch (IOException | RuntimeException e) {
            merged.close();
            throw e;
        }
    }

    private void append(PDDocument merged, File file) throws IOException {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")) {
            appendImage(merged, file);
        } else if (name.endsWith(".pdf")) {
            appendPdf(merged, file);
        } else {
            log.warn("Nicht unterstütztes Dateiformat, wird übersprungen: {}", file.getName());
        }
    }

    private void appendImage(PDDocument merged, File file) throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        merged.addPage(page);

        PDImageXObject image = PDImageXObject.createFromFileByContent(file, merged);
        ImagePlacement placement = ImagePlacement.centeredOn(page.getMediaBox(), image.getWidth(), image.getHeight());

        try (PDPageContentStream contentStream = new PDPageContentStream(merged, page)) {
            contentStream.drawImage(image, placement.x(), placement.y(), placement.width(), placement.height());
        }
    }

    private void appendPdf(PDDocument merged, File file) throws IOException {
        // PDDocument.importPage() lässt eingebettete Schriftdaten teilweise ungelesen,
        // sobald die Quelldatei danach geschlossen wird - PDFMergerUtility kopiert korrekt.
        try (PDDocument source = Loader.loadPDF(file)) {
            pdfMerger.appendDocument(merged, source);
        }
    }
}
