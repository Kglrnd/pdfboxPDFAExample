package de.kugidev.pdf.pdfa;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

class ExamplePDFAOnebTest {
    ExamplePDFAOneb examplePDFAOneb = new ExamplePDFAOneb();

    @Test
    void testConvertToPDFA1b() throws IOException {
        examplePDFAOneb.convertToPDFA1b(List.of(new File(getClass().getResource("classpath:color_test_800x600_118kb.jpg").getFile())), new File(getClass().getResource("classpath:sample-report.pdf").getFile()), "colorProfilePath");
    }
}
