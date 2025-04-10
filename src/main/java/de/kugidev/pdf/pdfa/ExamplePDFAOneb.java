package de.kugidev.pdf.pdfa;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;

import javax.xml.transform.TransformerException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

@Slf4j
public class ExamplePDFAOneb {

    public static final String PDF_BOX = "PDFBox";
    public static final String S_RGB_IEC_61966_2_1 = "sRGB IEC61966-2.1";

    /**
     * Konvertiert verschiedene Dateiformate (JPG, PNG, PDF) zu einem PDF/A-1b Dokument
     *
     * @param inputFiles Liste der Eingabedateien (JPG, PNG, PDF)
     * @param outputFile Ausgabedatei (PDF/A-1b)
     * @throws IOException Bei Fehlern während der Konvertierung
     */
    public void convertToPDFA1b(List<File> inputFiles, File outputFile) throws IOException {
        // Schritt 1: Alle Dateien zu einem PDF zusammenführen
        PDDocument mergedDocument = mergeFilesToPdf(inputFiles);

        // Schritt 2: Das zusammengeführte PDF zu PDF/A-1b konvertieren
        convertToPDFA(mergedDocument, outputFile);
    }

    /**
     * Fügt verschiedene Dateiformate (JPG, PNG, PDF) zu einem PDF zusammen
     *
     * @param inputFiles Liste der Eingabedateien
     * @return Zusammengeführtes PDF-Dokument
     * @throws IOException Bei Fehlern während des Zusammenführens
     */
    private PDDocument mergeFilesToPdf(List<File> inputFiles) throws IOException {
        PDDocument mergedDocument = new PDDocument();

        for (File file : inputFiles) {
            String fileName = file.getName().toLowerCase();

            if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".png")) {
                // Bild-Datei zum PDF hinzufügen
                PDPage page = new PDPage(PDRectangle.A4);
                mergedDocument.addPage(page);

                PDImageXObject image = PDImageXObject.createFromFileByContent(file, mergedDocument);

                // Bildgröße an A4-Seite anpassen
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float imageWidth = image.getWidth();
                float imageHeight = image.getHeight();

                // Skalierungsfaktor berechnen, um das Bild an die Seite anzupassen
                float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight) * 0.9f;
                float scaledWidth = imageWidth * scale;
                float scaledHeight = imageHeight * scale;

                // Position des Bildes in der Mitte der Seite berechnen
                float xPosition = (pageWidth - scaledWidth) / 2;
                float yPosition = (pageHeight - scaledHeight) / 2;

                try (PDPageContentStream contentStream = new PDPageContentStream(mergedDocument, page)) {
                    contentStream.drawImage(image, xPosition, yPosition, scaledWidth, scaledHeight);
                }
            } else if (fileName.endsWith(".pdf")) {
                // PDF-Datei zum zusammengeführten PDF hinzufügen
                try (PDDocument pdfDocument = Loader.loadPDF(file)) {
                    for (int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
                        PDPage page = pdfDocument.getPage(i);
                        PDPage importedPage = mergedDocument.importPage(page);
                        // Seitenattribute beibehalten
                        importedPage.setCropBox(page.getCropBox());
                        importedPage.setMediaBox(page.getMediaBox());
                        importedPage.setRotation(page.getRotation());
                    }
                }
            } else {
                log.warn("Nicht unterstütztes Dateiformat: {}", fileName);
            }
        }

        return mergedDocument;
    }

    /**
     * Konvertiert ein PDF-Dokument zu einem PDF/A-1b-konformen Dokument
     *
     * @param document   Das zu konvertierende PDF-Dokument
     * @param outputFile Die Ausgabedatei
     * @throws IOException Bei Fehlern während der Konvertierung
     */
    private void convertToPDFA(PDDocument document, File outputFile) throws IOException {
        // Dokument-Informationen setzen
        document.getDocumentInformation().setTitle("PDF/A-1b Dokument");
        document.getDocumentInformation().setCreator(PDF_BOX);
        document.getDocumentInformation().setProducer(PDF_BOX);

        // Die neuen Korrekturfunktionen aufrufen
        fixExtGStateTransparency(document);

        // Bestehende fixProhibitedTransparencyGroups-Methode aufrufen
        fixProhibitedTransparencyGroups(document);

        // Füge Metadaten im XMP-Format mit XMPBox hinzu
        try {
            addXmpMetadata(document);
        } catch (BadFieldValueException e) {
            throw new IOException("Fehler beim Erstellen der XMP-Metadaten", e);
        }

        // Füge den OutputIntent hinzu (erforderlich für PDF/A)
        addOutputIntent(document);

        // PDF/A-1b Dokument speichern
        document.save(outputFile);
        document.close();
    }

    /**
     * Fügt XMP-Metadaten zum Dokument hinzu (erforderlich für PDF/A)
     * Verwendet XMPBox für die Erstellung der Metadaten
     *
     * @param document Das PDF-Dokument
     * @throws IOException            Bei Fehlern während des Hinzufügens der Metadaten
     * @throws BadFieldValueException Bei ungültigen Werten in den XMP-Metadaten
     */
    private void addXmpMetadata(PDDocument document) throws IOException, BadFieldValueException {
        // XMP-Metadaten mit XMPBox erstellen
        XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();

        // PDF/A-Identifikationsschema hinzufügen
        PDFAIdentificationSchema pdfaSchema = xmpMetadata.createAndAddPDFAIdentificationSchema();
        pdfaSchema.setPart(1); // PDF/A-1
        pdfaSchema.setConformance("B"); // Konformitätsstufe B

        // Dublin Core Schema hinzufügen
        DublinCoreSchema dcSchema = xmpMetadata.createAndAddDublinCoreSchema();
        dcSchema.setTitle("PDF/A-1b Dokument");

        // XMP Basic Schema hinzufügen
        XMPBasicSchema xmpBasicSchema = xmpMetadata.createAndAddXMPBasicSchema();
        xmpBasicSchema.setCreatorTool(PDF_BOX);
        xmpBasicSchema.setCreateDate(Calendar.getInstance());

        // Adobe PDF Schema hinzufügen
        AdobePDFSchema adobePDFSchema = xmpMetadata.createAndAddAdobePDFSchema();
        adobePDFSchema.setProducer(PDF_BOX);

        // XMP-Metadaten in einen Stream serialisieren
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmpSerializer serializer = new XmpSerializer();
        try {
            serializer.serialize(xmpMetadata, baos, true);
        } catch (TransformerException e) {
            log.error("Fehler beim Serialisieren der XMP-Metadaten", e);
        }

        // Metadaten zum Dokument hinzufügen
        PDMetadata metadata = new PDMetadata(document);
        metadata.importXMPMetadata(baos.toByteArray());
        document.getDocumentCatalog().setMetadata(metadata);
    }

    /**
     * Fügt einen OutputIntent zum Dokument hinzu (erforderlich für PDF/A)
     *
     * @param document Das PDF-Dokument
     * @throws IOException Bei Fehlern während des Hinzufügens des OutputIntents
     */
    private void addOutputIntent(PDDocument document) throws IOException {

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(Objects.requireNonNull(classLoader.getResource("sRGB_v4_ICC_preference.icc")).getFile());
        // Lade das ICC-Profil
        InputStream colorProfile = new FileInputStream(file);
        PDOutputIntent outputIntent = new PDOutputIntent(document, colorProfile);
        outputIntent.setInfo(S_RGB_IEC_61966_2_1);
        outputIntent.setOutputCondition(S_RGB_IEC_61966_2_1);
        outputIntent.setOutputConditionIdentifier(S_RGB_IEC_61966_2_1);
        outputIntent.setRegistryName("http://www.color.org");
        document.getDocumentCatalog().addOutputIntent(outputIntent);
        colorProfile.close();
    }

    /**
     * Entfernt verbotene Transparenzwerte aus Gruppenobjekten im PDF
     *
     * @param document Das zu bereinigende PDF-Dokument
     * @throws IOException Bei Fehlern während der Bearbeitung
     */
    private void fixProhibitedTransparencyGroups(PDDocument document) throws IOException {
        PDDocumentCatalog catalog = document.getDocumentCatalog();

        for (PDPage page : catalog.getPages()) {
            // Ressourcen der Seite abrufen
            PDResources resources = page.getResources();
            if (resources == null) continue;

            // XObjects durchsuchen (können Gruppen enthalten)
            Iterable<COSName> xObjectNames = resources.getXObjectNames();
            if (xObjectNames != null) {
                for (COSName name : xObjectNames) {
                    fixFormXObject(resources.getXObject(name));

                }
            }
        }
    }

    /**
     * Korrigiert verbotene Transparenzeinstellungen in Form-XObjects
     *
     * @param pdxObject Das zu korrigierende PDXObject
     * @throws IOException Bei Fehlern während der Bearbeitung
     */
    private void fixFormXObject(PDXObject pdxObject) throws IOException {
        COSDictionary dict = pdxObject.getCOSObject();
        COSBase groupObject = dict.getDictionaryObject(COSName.GROUP);

        // Pattern Matching für instanceof
        if (groupObject instanceof COSDictionary group) {
            COSName sKey = group.getCOSName(COSName.S);

            if (COSName.TRANSPARENCY.equals(sKey)) {
                group.removeItem(COSName.S);
            }
        }

        // Ressourcen über das COSDictionary direkt abrufen
        COSBase resourcesDict = dict.getDictionaryObject(COSName.RESOURCES);

        // Pattern Matching für instanceof
        if (resourcesDict instanceof COSDictionary resourcesCosDictionary) {
            PDResources resources = new PDResources(resourcesCosDictionary);
            Iterable<COSName> xObjectNames = resources.getXObjectNames();
            if (xObjectNames != null) {
                for (COSName name : xObjectNames) {
                    PDXObject xObject = resources.getXObject(name);
                    if (xObject != null) {
                        fixFormXObject(xObject);
                    }
                }
            }
        }
    }

    /**
     * Entfernt SMask-Einträge aus XObjects
     *
     * @param pdxObject Das zu korrigierende PDXObject
     * @throws IOException Bei Fehlern während der Bearbeitung
     */
    private void removeSMaskFromXObject(PDXObject pdxObject) throws IOException {
        if (pdxObject instanceof PDFormXObject form) {

            // SMask entfernen
            COSDictionary dict = form.getCOSObject();
            if (dict.containsKey(COSName.SMASK)) {
                dict.removeItem(COSName.SMASK);
            }

            // Ressourcen im Form XObject bearbeiten
            PDResources resources = form.getResources();
            if (resources != null) {
                // Verschachtelte XObjects bearbeiten
                for (COSName name : resources.getXObjectNames()) {
                    PDXObject xObject = resources.getXObject(name);
                    removeSMaskFromXObject(xObject);
                }
            }
        }
    }

    /**
     * Korrigiert ExtGState-Einträge, um CA/ca auf 1 zu setzen
     *
     * @param document Das zu korrigierende PDF-Dokument
     * @throws IOException Bei Fehlern während der Bearbeitung
     */
    private void fixExtGStateTransparency(PDDocument document) throws IOException {
        for (PDPage page : document.getPages()) {
            PDResources resources = page.getResources();
            if (resources != null) {
                processExtGStateEntries(resources);
                processXObjects(resources);
            }
        }
    }

    /**
     * Verarbeitet ExtGState-Einträge in den Ressourcen und setzt Alpha-Konstanten zurück
     *
     * @param resources Die zu verarbeitenden Ressourcen
     */
    private void processExtGStateEntries(PDResources resources) {
        for (COSName name : resources.getExtGStateNames()) {
            PDExtendedGraphicsState extGState = resources.getExtGState(name);
            if (extGState != null) {
                resetAlphaConstants(extGState);
            }
        }
    }

    /**
     * Verarbeitet XObjects in den Ressourcen und wendet Transparenz-Korrekturen an
     *
     * @param resources Die zu verarbeitenden Ressourcen
     * @throws IOException Bei Fehlern während der Verarbeitung
     */
    private void processXObjects(PDResources resources) throws IOException {
        for (COSName name : resources.getXObjectNames()) {
            PDXObject xObject = resources.getXObject(name);
            if (xObject instanceof PDFormXObject form) {
                PDResources formResources = form.getResources();
                if (formResources != null) {
                    processExtGStateEntries(formResources);
                }
                removeSMaskFromXObject(form);
            }
        }
    }

    /**
     * Setzt die Alpha-Konstanten für Strich- und Fülldeckkraft auf 1
     *
     * @param extGState Der zu bearbeitende ExtendedGraphicsState
     */
    private void resetAlphaConstants(PDExtendedGraphicsState extGState) {
        // CA (Strichdeckkraft) auf 1 setzen
        extGState.setStrokingAlphaConstant(1.0f);
        // ca (Fülldeckkraft) auf 1 setzen
        extGState.setNonStrokingAlphaConstant(1.0f);
    }
}
