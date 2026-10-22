package de.kugidev.pdf.pdfa;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Position und Größe, mit der ein Bild zentriert und seitenfüllend
 * (abzüglich Rand) auf einer PDF-Seite platziert wird.
 */
record ImagePlacement(float x, float y, float width, float height) {

    private static final float PAGE_MARGIN_FACTOR = 0.9f;

    static ImagePlacement centeredOn(PDRectangle page, float imageWidth, float imageHeight) {
        float scale = Math.min(page.getWidth() / imageWidth, page.getHeight() / imageHeight) * PAGE_MARGIN_FACTOR;
        float width = imageWidth * scale;
        float height = imageHeight * scale;
        float x = (page.getWidth() - width) / 2;
        float y = (page.getHeight() - height) / 2;
        return new ImagePlacement(x, y, width, height);
    }
}
