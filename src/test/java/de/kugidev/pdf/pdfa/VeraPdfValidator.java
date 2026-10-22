package de.kugidev.pdf.pdfa;

import org.verapdf.core.VeraPDFException;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.VeraPDFFoundry;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.validators.ValidatorFactory;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Validiert eine PDF-Datei mit der unabhängigen veraPDF-Implementierung (nicht PDFBox-basiert)
 * gegen ein PDF/A-Profil. Dient als Test-Nachweis, dass die von {@link PdfA2bConverter} erzeugten
 * Dokumente tatsächlich konform sind, statt dies nur über die eigenen XMP-Metadaten zu behaupten.
 */
final class VeraPdfValidator {

    static {
        VeraGreenfieldFoundryProvider.initialise();
    }

    private VeraPdfValidator() {
    }

    static ValidationResult validate(File pdfFile, PDFAFlavour flavour) throws IOException {
        VeraPDFFoundry foundry = Foundries.defaultInstance();
        PDFAValidator validator = ValidatorFactory.createValidator(flavour, false);
        try (PDFAParser parser = foundry.createParser(pdfFile, flavour)) {
            return validator.validate(parser);
        } catch (VeraPDFException e) {
            throw new IOException("veraPDF-Validierung fehlgeschlagen", e);
        }
    }

    static String describeFailures(ValidationResult result) {
        return result.getTestAssertions().stream()
                .filter(assertion -> assertion.getStatus() == TestAssertion.Status.FAILED)
                .map(assertion -> assertion.getRuleId() + ": " + assertion.getMessage()
                        + " (" + assertion.getLocationContext() + ")")
                .collect(Collectors.joining("\n"));
    }
}
