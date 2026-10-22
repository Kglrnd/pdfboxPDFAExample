package de.kugidev.pdf.pdfa;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;

import java.io.IOException;

/**
 * Prüft Grundanforderungen von PDF/A, die XMP-Metadaten allein nicht garantieren:
 * keine Verschlüsselung, alle verwendeten Schriftarten eingebettet.
 */
final class PdfACompliance {

    private PdfACompliance() {
    }

    static void requireNotEncrypted(PDDocument document) throws PdfAComplianceException {
        if (document.isEncrypted()) {
            throw new PdfAComplianceException("PDF/A verbietet verschlüsselte Dokumente.");
        }
    }

    static void requireAllFontsEmbedded(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            requireFontsEmbedded(page.getResources());
        }
    }

    private static void requireFontsEmbedded(PDResources resources) throws IOException {
        if (resources == null) {
            return;
        }
        for (COSName name : resources.getFontNames()) {
            PDFont font = resources.getFont(name);
            if (font != null && !font.isEmbedded()) {
                throw new PdfAComplianceException(
                        "Nicht eingebettete Schriftart gefunden: " + font.getName()
                                + ". PDF/A-2b erfordert eingebettete Schriftarten.");
            }
        }
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (xObject instanceof PDFormXObject form) {
                requireFontsEmbedded(form.getResources());
            }
        }
    }
}
