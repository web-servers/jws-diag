package org.jboss.jws.diag.config;

import org.jboss.jws.diag.config.formatter.ConfigHumanFormatter;
import org.jboss.jws.diag.config.formatter.ConfigJsonFormatter;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.parser.PropertyResolver;
import org.jboss.jws.diag.config.parser.ServerXmlParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that sensitive credential values never appear in formatted output.
 */
class ConfigSecurityTest {

    private static ServerConfig parseFixture(String fileName) throws IOException, URISyntaxException {
        Path path = Paths.get(ConfigSecurityTest.class.getClassLoader()
                .getResource("fixtures/config/" + fileName).toURI());
        PropertyResolver resolver = new PropertyResolver(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        return new ServerXmlParser(resolver).parse(path);
    }

    @Test
    void jsonOutputDoesNotContainKeystorePassword() throws Exception {
        ServerConfig config = parseFixture("server-full-tls.xml");
        String json = new ConfigJsonFormatter().format(config);
        assertThat(json).doesNotContain("s3cretP@ss");
        assertThat(json).contains("***REDACTED***");
    }

    @Test
    void humanOutputDoesNotContainKeystorePassword() throws Exception {
        ServerConfig config = parseFixture("server-full-tls.xml");
        String output = new ConfigHumanFormatter().format(config);
        assertThat(output).doesNotContain("s3cretP@ss");
        assertThat(output).contains("***REDACTED***");
    }

    @Test
    void vaultTokenPreservedInJsonOutput() throws Exception {
        ServerConfig config = parseFixture("server-vault-tls.xml");
        String json = new ConfigJsonFormatter().format(config);
        assertThat(json).contains("${VAULT::ssl::keystorePassword::1}");
        assertThat(json).doesNotContain("***REDACTED***");
    }

    @Test
    void vaultTokenPreservedInHumanOutput() throws Exception {
        ServerConfig config = parseFixture("server-vault-tls.xml");
        String output = new ConfigHumanFormatter().format(config);
        assertThat(output).contains("${VAULT::ssl::keystorePassword::1}");
        assertThat(output).doesNotContain("***REDACTED***");
    }

    @Test
    void jsonOutputDoesNotContainVaultResolvedValues() throws Exception {
        // VAULT tokens must stay opaque — never resolved to actual secrets
        ServerConfig config = parseFixture("server-vault-refs.xml");
        String json = new ConfigJsonFormatter().format(config);
        // The VAULT token itself is preserved, not resolved
        assertThat(json).contains("${VAULT::");
    }
}
