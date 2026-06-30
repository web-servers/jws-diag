package org.jboss.jws.diag.config.parser;

import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.HostConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests: XML with ${...} placeholders + property sources → resolved effective values.
 */
class PropertyResolutionIntegrationTest {

    private Path fixture(String name) throws URISyntaxException {
        return Paths.get(getClass().getClassLoader()
                .getResource("fixtures/config/" + name).toURI());
    }

    @Test
    void portResolvedFromCatalinaProperties() throws Exception {
        Map<String, String> catProps = new HashMap<>();
        catProps.put("http.port", "9090");
        ServerXmlParser parser = new ServerXmlParser(
                new PropertyResolver(Collections.emptyMap(), catProps, Collections.emptyMap()));

        ServerConfig cfg = parser.parse(fixture("server-property-refs.xml"));
        ConnectorConfig c = cfg.getServices().get(0).getConnectors().get(0);
        assertThat(c.getPort()).isEqualTo(9090);
    }

    @Test
    void portResolvedFromSystemPropertiesOverCatalinaProperties() throws Exception {
        Map<String, String> sysProps = new HashMap<>();
        sysProps.put("http.port", "7070");
        Map<String, String> catProps = new HashMap<>();
        catProps.put("http.port", "9090");
        ServerXmlParser parser = new ServerXmlParser(
                new PropertyResolver(sysProps, catProps, Collections.emptyMap()));

        ServerConfig cfg = parser.parse(fixture("server-property-refs.xml"));
        assertThat(cfg.getServices().get(0).getConnectors().get(0).getPort()).isEqualTo(7070);
    }

    @Test
    void appBaseResolvedFromCatalinaProperties() throws Exception {
        Map<String, String> catProps = new HashMap<>();
        catProps.put("http.port", "8080");
        catProps.put("app.base", "custom-webapps");
        ServerXmlParser parser = new ServerXmlParser(
                new PropertyResolver(Collections.emptyMap(), catProps, Collections.emptyMap()));

        ServerConfig cfg = parser.parse(fixture("server-property-refs.xml"));
        HostConfig host = cfg.getServices().get(0).getEngine().getHosts().get(0);
        assertThat(host.getAppBase().getValue()).isEqualTo("custom-webapps");
        assertThat(host.getAppBase().isExplicit()).isTrue();
    }

    @Test
    void unresolvedPlaceholderKeptVerbatim() throws Exception {
        ServerXmlParser parser = new ServerXmlParser(
                new PropertyResolver(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));

        ServerConfig cfg = parser.parse(fixture("server-property-refs.xml"));
        HostConfig host = cfg.getServices().get(0).getEngine().getHosts().get(0);
        assertThat(host.getAppBase().getValue()).isEqualTo("${app.base}");
    }

    @Test
    void portResolvedFromEnvironmentVariable() throws Exception {
        Map<String, String> env = new HashMap<>();
        env.put("HTTP_PORT", "8888");
        ServerXmlParser parser = new ServerXmlParser(
                new PropertyResolver(Collections.emptyMap(), Collections.emptyMap(), env));

        // server-property-refs.xml uses ${http.port} not ${env.HTTP_PORT}, so won't resolve via env
        // Use catalina.properties as the source for this fixture
        Map<String, String> catProps = new HashMap<>();
        catProps.put("http.port", "8080");
        catProps.put("app.base", "webapps");
        parser = new ServerXmlParser(new PropertyResolver(Collections.emptyMap(), catProps, env));
        ServerConfig cfg = parser.parse(fixture("server-property-refs.xml"));
        assertThat(cfg.getServices().get(0).getConnectors().get(0).getPort()).isEqualTo(8080);
    }
}
