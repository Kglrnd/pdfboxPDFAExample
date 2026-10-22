package de.kugidev.pdf.pdfa;

import java.io.IOException;

/**
 * Signalisiert, dass das erzeugte Dokument eine Grundanforderung von PDF/A
 * verletzt (z.B. nicht eingebettete Schriftart, Verschlüsselung).
 */
public class PdfAComplianceException extends IOException {

    public PdfAComplianceException(String message) {
        super(message);
    }
}
