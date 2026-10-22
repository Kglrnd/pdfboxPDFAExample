package de.kugidev.pdf.pdfa;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

/**
 * Schreibt die für PDF/A-2b erforderlichen Metadaten: XMP-Identifikation,
 * Dokument-Information (synchron zu den XMP-Werten) und den ICC-OutputIntent.
 */
class PdfAMetadataWriter {

    private static final int PDFA_PART = 2;
    private static final String PDFA_CONFORMANCE = "B";

    private static final String PRODUCER = "PDFBox";
    private static final String TITLE = "PDF/A-2b Dokument";

    private static final String SRGB_PROFILE_NAME = "sRGB IEC61966-2.1";
    // Muss Device Class "mntr" oder "prtr" haben (ISO 19005-2, 6.2.3) - die zuvor verwendete
    // "_preference"-Variante ist Device Class "spac" und wird von PDF/A-Validatoren abgelehnt.
    private static final String SRGB_PROFILE_RESOURCE = "sRGB2014.icc";

    void apply(PDDocument document) throws IOException {
        Calendar now = Calendar.getInstance();
        writeDocumentInformation(document, now);
        writeXmpMetadata(document, now);
        addOutputIntent(document);
    }

    private void writeDocumentInformation(PDDocument document, Calendar now) {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(TITLE);
        info.setCreator(PRODUCER);
        info.setProducer(PRODUCER);
        info.setCreationDate(now);
        info.setModificationDate(now);
    }

    private void writeXmpMetadata(PDDocument document, Calendar now) throws IOException {
        XMPMetadata xmp = XMPMetadata.createXMPMetadata();
        try {
            PDFAIdentificationSchema pdfaSchema = xmp.createAndAddPDFAIdentificationSchema();
            pdfaSchema.setPart(PDFA_PART);
            pdfaSchema.setConformance(PDFA_CONFORMANCE);

            DublinCoreSchema dublinCore = xmp.createAndAddDublinCoreSchema();
            dublinCore.setTitle(TITLE);

            XMPBasicSchema basicSchema = xmp.createAndAddXMPBasicSchema();
            basicSchema.setCreatorTool(PRODUCER);
            basicSchema.setCreateDate(now);
            basicSchema.setModifyDate(now);

            AdobePDFSchema adobeSchema = xmp.createAndAddAdobePDFSchema();
            adobeSchema.setProducer(PRODUCER);
        } catch (BadFieldValueException e) {
            throw new IOException("Ungültiger Wert in den XMP-Metadaten", e);
        }

        ByteArrayOutputStream serialized = new ByteArrayOutputStream();
        try {
            new XmpSerializer().serialize(xmp, serialized, true);
        } catch (TransformerException e) {
            throw new IOException("Fehler beim Serialisieren der XMP-Metadaten", e);
        }

        PDMetadata metadata = new PDMetadata(document);
        metadata.importXMPMetadata(serialized.toByteArray());
        document.getDocumentCatalog().setMetadata(metadata);
    }

    private void addOutputIntent(PDDocument document) throws IOException {
        try (InputStream colorProfile = getClass().getClassLoader().getResourceAsStream(SRGB_PROFILE_RESOURCE)) {
            if (colorProfile == null) {
                throw new IOException("ICC-Farbprofil nicht gefunden: " + SRGB_PROFILE_RESOURCE);
            }
            PDOutputIntent outputIntent = new PDOutputIntent(document, colorProfile);
            outputIntent.setInfo(SRGB_PROFILE_NAME);
            outputIntent.setOutputCondition(SRGB_PROFILE_NAME);
            outputIntent.setOutputConditionIdentifier(SRGB_PROFILE_NAME);
            outputIntent.setRegistryName("http://www.color.org");
            document.getDocumentCatalog().addOutputIntent(outputIntent);
        }
    }
}
