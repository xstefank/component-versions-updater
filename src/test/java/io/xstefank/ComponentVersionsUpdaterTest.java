package io.xstefank;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

@QuarkusTest
public class ComponentVersionsUpdaterTest {

    private ComponentVersionsUpdaterMain componentVersionsUpdaterMain;

    @BeforeEach
    public void beforeEach() {
        componentVersionsUpdaterMain = new ComponentVersionsUpdaterMain();
    }

    @Test
    public void validUpdateTest() throws IOException {
        Path pomFileOriginal = Path.of("src/test/resources/files/pom.xml");
        Path testFilePath = Files.copy(pomFileOriginal, Path.of("target/tested-pom.xml"), StandardCopyOption.REPLACE_EXISTING);

        componentVersionsUpdaterMain.inputFileArg = "src/test/resources/files/correct-component-versions.yml";
        componentVersionsUpdaterMain.pomFileArg = testFilePath.toString();

        componentVersionsUpdaterMain.call();

        Assertions.assertTrue(Arrays.equals(Files.readAllBytes(Path.of("src/test/resources/files/updated-pom.xml")),
            Files.readAllBytes(testFilePath)), "The versions in pom.xml weren't updated correctly");
    }

    @Test
    public void invalidComponentVersionsFileTest() throws Exception {
        componentVersionsUpdaterMain.inputFileArg = "src/test/resources/files/incorrect-component-version.yml";
        componentVersionsUpdaterMain.pomFileArg = "src/test/resources/files/pom.xml";

        Assertions.assertThrows(IllegalArgumentException.class, () -> componentVersionsUpdaterMain.call());
    }
}
