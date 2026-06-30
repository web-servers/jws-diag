package org.jboss.jws.diag.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jws.diag.config.formatter.ConfigJsonFormatter;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.parser.PropertyResolver;
import org.jboss.jws.diag.config.parser.ServerXmlParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Golden output tests for the config command JSON formatter.
 *
 * <p>On the first run, if a golden file does not exist it is generated from
 * the current parser output and the test passes (capturing baseline). On
 * subsequent runs the generated output must match the stored golden file.
 *
 * <p>To regenerate all golden files, delete them and re-run {@code mvn verify}.
 */
class ConfigGoldenTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Path GOLDEN_DIR =
            Paths.get("src/test/resources/golden/config");

    @ParameterizedTest
    @ValueSource(strings = {
            "server-valid-basic",
            "server-all-defaults",
            "server-multi-connector",
            "server-executor",
            "server-proxy-valve",
            "server-vault-refs",
            "server-multi-service",
            "server-property-refs",
            "server-full-tls",
            "server-vault-tls"
    })
    void goldenJsonOutputMatchesExpected(String fixtureName) throws Exception {
        ServerConfig config = parseFixture(fixtureName + ".xml");
        String actual = new ConfigJsonFormatter().format(config);

        Path goldenFile = GOLDEN_DIR.resolve(fixtureName + ".json");
        if (!Files.exists(goldenFile)) {
            Files.createDirectories(goldenFile.getParent());
            Files.writeString(goldenFile, actual);
            // First run: golden file created; comparison deferred to next run.
            return;
        }

        JsonNode expected = MAPPER.readTree(goldenFile.toFile());
        JsonNode actualNode = MAPPER.readTree(actual);
        assertThat(actualNode)
                .as("JSON output for %s differs from golden file", fixtureName)
                .isEqualTo(expected);
    }

    private static ServerConfig parseFixture(String fileName) throws IOException, URISyntaxException {
        Path path = Paths.get(ConfigGoldenTest.class.getClassLoader()
                .getResource("fixtures/config/" + fileName).toURI());
        PropertyResolver resolver = new PropertyResolver(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        return new ServerXmlParser(resolver).parse(path);
    }
}
