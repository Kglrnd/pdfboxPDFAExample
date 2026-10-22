package de.kugidev.pdf.pdfa;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.ValidationResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PdfA2bConverterTest {

    private final PdfA2bConverter converter = new PdfA2bConverter();

    @Test
    void convertMergesPdfAndImageIntoPdfA2b(@org.junit.jupiter.api.io.TempDir Path tempDir) throws IOException {
        Path resourceDirectory = Paths.get("src", "test", "resources");
        File pdf = resourceDirectory.resolve("sample-report.pdf").toFile();
        File jpg = resourceDirectory.resolve("color_test_800x600_118kb.jpg").toFile();
        File outputFile = tempDir.resolve("output.pdf").toFile();

        converter.convert(List.of(pdf, jpg), outputFile);

        assertThat(outputFile).exists();
        try (PDDocument result = Loader.loadPDF(outputFile)) {
            // sample-report.pdf Seiten + eine Seite für das Bild
            assertThat(result.getNumberOfPages()).isGreaterThan(1);
            assertThat(result.getDocumentCatalog().getMetadata()).isNotNull();
            assertThat(result.getDocumentCatalog().getOutputIntents()).isNotEmpty();
            assertThat(result.getVersion()).isEqualTo(1.7f);
        }

        // Unabhängige Prüfung mit veraPDF (nicht PDFBox-basiert) als Nachweis echter PDF/A-2b-Konformität
        ValidationResult validationResult = VeraPdfValidator.validate(outputFile, PDFAFlavour.PDFA_2_B);
        assertThat(validationResult.isCompliant())
                .withFailMessage("veraPDF meldet Verstöße:%n%s", VeraPdfValidator.describeFailures(validationResult))
                .isTrue();
    }
}
