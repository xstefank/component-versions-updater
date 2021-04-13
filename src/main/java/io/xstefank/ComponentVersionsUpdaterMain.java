package io.xstefank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.xstefank.model.yml.Component;
import io.xstefank.model.yml.ComponentVersions;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

@QuarkusMain
@CommandLine.Command(name = "component-versions-updater", mixinStandardHelpOptions = true, version = "1.0.0")
public class ComponentVersionsUpdaterMain implements Callable<Integer>, QuarkusApplication {

    private final Logger logger = Logger.getLogger(ComponentVersionsUpdaterMain.class);
    private final String homeDir = System.getProperty("user.home");

    @Inject
    CommandLine.IFactory factory;

    @CommandLine.Parameters(index = "0", description = "The input YAML file containing the groupId and versions strings. Output of nexus-component-processor.")
    String inputFileArg;

    @CommandLine.Parameters(index = "1", description = "The pom.xml file to be updated.")
    String pomFileArg;

    private File pomFile;

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).execute(args);
    }

    @Override
    public Integer call() throws IOException {
        File inputFile = Paths.get(expandHomeDir(inputFileArg)).toFile();
        pomFile = Paths.get(expandHomeDir(pomFileArg)).toFile();

        logger.infof("Processing components from %s and updating pom.xml at %s", inputFile.getCanonicalPath(), pomFile.getCanonicalPath());

        ComponentVersions componentVersionsYaml = readComponentVersions(inputFile);

        componentVersionsYaml.components.forEach(this::updateVersion);

        return 0;
    }

    private ComponentVersions readComponentVersions(File inputFile) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.readValue(inputFile, ComponentVersions.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot parse the input file " + inputFile.getAbsolutePath(), e);
        }
    }

    private void updateVersion(Component component) {
        logger.infof("Updating %s to %s", component.groupId, component.version);

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sed", "-i",
            String.format("s#<version.%s>.*</version.%s>#<version.%s>%s</version.%s>#",
                component.groupId, component.groupId, component.groupId, component.version, component.groupId),
            pomFile.getAbsolutePath());

        try {
            Process process = processBuilder.start();

            StringBuilder err = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                err.append(line + "\n");
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.debug(err);
                logger.debug("Command exit code was " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Could not process sed command", e);
        }

    }

    private String expandHomeDir(String path) {
        return path.replaceFirst("^~", homeDir);
    }
}
