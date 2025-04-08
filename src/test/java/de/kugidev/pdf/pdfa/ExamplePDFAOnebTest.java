package de.kugidev.pdf.pdfa;

import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class ExamplePDFAOnebTest {
    ExamplePDFAOneb examplePDFAOneb = new ExamplePDFAOneb();

    @Test
    void testConvertToPDFA1b() throws IOException {
        Path resourceDirectory = Paths.get("src","test", "resources");
        String absolutePath = resourceDirectory.toFile().getAbsolutePath();
        File pdf = Paths.get(absolutePath, "sample-report.pdf").toFile();
        File jpg = Paths.get(absolutePath, "color_test_800x600_118kb.jpg").toFile();

     List<File> files = List.of(pdf, jpg);

     File testpdfa = Files.newFile("test.pdf");

     examplePDFAOneb.convertToPDFA1b(files, testpdfa);

     assertThat(testpdfa).isNotNull();

    }
}
