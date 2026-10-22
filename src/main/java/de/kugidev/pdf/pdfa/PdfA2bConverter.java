package de.kugidev.pdf.pdfa;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Erzeugt aus JPG-, PNG- und PDF-Eingabedateien ein PDF/A-2b-konformes PDF-Dokument.
 * <p>
 * Es werden nur Apache-2.0-lizenzierte Bibliotheken (PDFBox, XMPBox) verwendet, keine
 * Komponenten, die bei professioneller Nutzung kostenpflichtig würden.
 */
public final class PdfA2bConverter {

    private static final float PDF_VERSION = 1.7f;

    private final SourceDocumentMerger merger = new SourceDocumentMerger();
    private final PdfAMetadataWriter metadataWriter = new PdfAMetadataWriter();

    /**
     * Führt die Eingabedateien zusammen und schreibt das Ergebnis als PDF/A-2b nach
     * {@code outputFile}.
     *
     * @throws PdfAComplianceException wenn das Ergebnis eine PDF/A-Grundanforderung verletzt
     *                                  (verschlüsselt oder nicht eingebettete Schriftart)
     */
    public void convert(List<File> inputFiles, File outputFile) throws IOException {
        try (PDDocument document = merger.merge(inputFiles)) {
            metadataWriter.apply(document);

            PdfACompliance.requireNotEncrypted(document);
            PdfACompliance.requireAllFontsEmbedded(document);

            document.setVersion(PDF_VERSION);
            document.save(outputFile);
        }
    }
}
