package org.jboss.jws.diag.config.parser;

import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.EngineConfig;
import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.HostConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.model.ServiceConfig;
import org.jboss.jws.diag.config.model.ValveConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerXmlParserTest {

    private ServerXmlParser parser;

    @BeforeEach
    void setUp() {
        PropertyResolver resolver = new PropertyResolver(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        parser = new ServerXmlParser(resolver);
    }

    private Path fixture(String name) {
        try {
            return Paths.get(getClass().getClassLoader()
                    .getResource("fixtures/config/" + name).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void basicFileParseSucceeds() throws IOException {
        ServerConfig cfg = parser.parse(fixture("server-valid-basic.xml"));
        assertThat(cfg).isNotNull();
    }

    @Test
    void basicFileShutdownAttributes() throws IOException {
        ServerConfig cfg = parser.parse(fixture("server-valid-basic.xml"));
        assertThat(cfg.getShutdownPort()).isEqualTo(8005);
        assertThat(cfg.getShutdownCommand()).isEqualTo("SHUTDOWN");
    }

    @Test
    void basicFileListenersParsed() throws IOException {
        ServerConfig cfg = parser.parse(fixture("server-valid-basic.xml"));
        assertThat(cfg.getListeners()).hasSize(2);
        assertThat(cfg.getListeners().get(0).getClassName())
                .isEqualTo("org.apache.catalina.startup.VersionLoggerListener");
    }

    @Test
    void basicFileServiceParsed() throws IOException {
        ServerConfig cfg = parser.parse(fixture("server-valid-basic.xml"));
        assertThat(cfg.getServices()).hasSize(1);
        assertThat(cfg.getServices().get(0).getName()).isEqualTo("Catalina");
    }

    @Test
    void basicFileConnectorExplicitAttributes() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-valid-basic.xml")).getServices().get(0);
        ConnectorConfig conn = svc.getConnectors().get(0);

        assertThat(conn.getPort()).isEqualTo(8080);
        assertThat(conn.getProtocol().getValue()).isEqualTo("HTTP/1.1");
        assertThat(conn.getProtocol().isExplicit()).isTrue();
        assertThat(conn.getConnectionTimeout().getValue()).isEqualTo(20000);
        assertThat(conn.getConnectionTimeout().isExplicit()).isTrue();
    }

    @Test
    void basicFileConnectorDefaultsFilledIn() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-valid-basic.xml")).getServices().get(0);
        ConnectorConfig conn = svc.getConnectors().get(0);

        assertThat(conn.getMaxThreads().getValue()).isEqualTo(200);
        assertThat(conn.getMaxThreads().isExplicit()).isFalse();
        assertThat(conn.getMaxConnections().getValue()).isEqualTo(8192);
        assertThat(conn.getMaxConnections().isExplicit()).isFalse();
        assertThat(conn.getSslEnabled().getValue()).isFalse();
        assertThat(conn.getSslEnabled().isExplicit()).isFalse();
    }

    @Test
    void basicFileHostParsed() throws IOException {
        EngineConfig engine = parser.parse(fixture("server-valid-basic.xml"))
                .getServices().get(0).getEngine();
        assertThat(engine.getName()).isEqualTo("Catalina");
        assertThat(engine.getDefaultHost()).isEqualTo("localhost");

        HostConfig host = engine.getHosts().get(0);
        assertThat(host.getName()).isEqualTo("localhost");
        assertThat(host.getAppBase().getValue()).isEqualTo("webapps");
        assertThat(host.getAppBase().isExplicit()).isTrue();
        assertThat(host.getAutoDeploy().getValue()).isTrue();
        assertThat(host.getAutoDeploy().isExplicit()).isTrue();
    }

    @Test
    void allDefaultsConnectorOnlyPortIsExplicit() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-all-defaults.xml")).getServices().get(0);
        ConnectorConfig conn = svc.getConnectors().get(0);

        assertThat(conn.getPort()).isEqualTo(8443);
        assertThat(conn.getProtocol().getValue()).isEqualTo("HTTP/1.1");
        assertThat(conn.getProtocol().isExplicit()).isFalse();
        assertThat(conn.getMaxThreads().getValue()).isEqualTo(200);
        assertThat(conn.getMaxThreads().isExplicit()).isFalse();
    }

    @Test
    void allDefaultsHostDefaultsApplied() throws IOException {
        HostConfig host = parser.parse(fixture("server-all-defaults.xml"))
                .getServices().get(0).getEngine().getHosts().get(0);

        assertThat(host.getAppBase().getValue()).isEqualTo("webapps");
        assertThat(host.getAppBase().isExplicit()).isFalse();
        assertThat(host.getAutoDeploy().getValue()).isTrue();
        assertThat(host.getAutoDeploy().isExplicit()).isFalse();
        assertThat(host.getUnpackWARs().getValue()).isTrue();
        assertThat(host.getUnpackWARs().isExplicit()).isFalse();
    }

    @Test
    void multiConnectorAllThreeParsed() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-multi-connector.xml")).getServices().get(0);
        assertThat(svc.getConnectors()).hasSize(3);
    }

    @Test
    void multiConnectorHttpConnector() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-multi-connector.xml")).getServices().get(0);
        ConnectorConfig http = svc.getConnectors().get(0);
        assertThat(http.getPort()).isEqualTo(8080);
        assertThat(http.getSslEnabled().getValue()).isFalse();
    }

    @Test
    void multiConnectorSslConnectorAttributes() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-multi-connector.xml")).getServices().get(0);
        ConnectorConfig ssl = svc.getConnectors().get(1);

        assertThat(ssl.getPort()).isEqualTo(8443);
        assertThat(ssl.getSslEnabled().getValue()).isTrue();
        assertThat(ssl.getSslEnabled().isExplicit()).isTrue();
        assertThat(ssl.getMaxThreads().getValue()).isEqualTo(150);
        assertThat(ssl.getMaxThreads().isExplicit()).isTrue();
    }

    @Test
    void multiConnectorAjpConnector() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-multi-connector.xml")).getServices().get(0);
        ConnectorConfig ajp = svc.getConnectors().get(2);

        assertThat(ajp.getPort()).isEqualTo(8009);
        assertThat(ajp.getProtocol().getValue()).isEqualTo("AJP/1.3");
        assertThat(ajp.getProtocol().isExplicit()).isTrue();
    }

    @Test
    void ajpConnectorGetsSecretRequiredDefault() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-multi-connector.xml")).getServices().get(0);
        ConnectorConfig ajp = svc.getConnectors().get(2);

        assertThat(ajp.getSecretRequired()).isNotNull();
        assertThat(ajp.getSecretRequired().getValue()).isTrue();
        assertThat(ajp.getSecretRequired().isExplicit()).isFalse();
    }

    @Test
    void httpConnectorDoesNotHaveSecretRequired() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-multi-connector.xml")).getServices().get(0);
        ConnectorConfig http = svc.getConnectors().get(0);

        assertThat(http.getSecretRequired()).isNull();
    }

    @Test
    void compressionParsedAsString() throws IOException {
        // server-proxy-valve.xml has no compression attr → default "off"
        ServiceConfig svc = parser.parse(fixture("server-valid-basic.xml")).getServices().get(0);
        ConnectorConfig c = svc.getConnectors().get(0);

        assertThat(c.getCompression().getValue()).isEqualTo("off");
        assertThat(c.getCompression().isExplicit()).isFalse();
    }

    @Test
    void executorParsed() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-executor.xml")).getServices().get(0);
        assertThat(svc.getExecutors()).hasSize(1);

        ExecutorConfig exec = svc.getExecutors().get(0);
        assertThat(exec.getName()).isEqualTo("tomcatThreadPool");
        assertThat(exec.getNamePrefix()).isEqualTo("catalina-exec-");
        assertThat(exec.getMaxThreads().getValue()).isEqualTo(150);
        assertThat(exec.getMaxThreads().isExplicit()).isTrue();
        assertThat(exec.getMinSpareThreads().getValue()).isEqualTo(4);
        assertThat(exec.getMinSpareThreads().isExplicit()).isTrue();
    }

    @Test
    void connectorExecutorRefParsed() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-executor.xml")).getServices().get(0);
        ConnectorConfig conn = svc.getConnectors().get(0);
        assertThat(conn.getExecutorRef()).isEqualTo("tomcatThreadPool");
    }

    @Test
    void proxyAttributesParsed() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-proxy-valve.xml")).getServices().get(0);
        ConnectorConfig conn = svc.getConnectors().get(0);

        assertThat(conn.getProxyName()).isEqualTo("example.com");
        assertThat(conn.getProxyPort()).isEqualTo(80);
    }

    @Test
    void valvesParsed() throws IOException {
        HostConfig host = parser.parse(fixture("server-proxy-valve.xml"))
                .getServices().get(0).getEngine().getHosts().get(0);

        assertThat(host.getValves()).hasSize(2);

        ValveConfig accessLog = host.getValves().get(0);
        assertThat(accessLog.getClassName())
                .isEqualTo("org.apache.catalina.valves.AccessLogValve");
        assertThat(accessLog.getAttributes().get("directory")).isEqualTo("logs");

        ValveConfig remoteIp = host.getValves().get(1);
        assertThat(remoteIp.getClassName())
                .isEqualTo("org.apache.catalina.valves.RemoteIpValve");
        assertThat(remoteIp.getAttributes().get("remoteIpHeader"))
                .isEqualTo("X-Forwarded-For");
    }

    @Test
    void vaultTokenPreservedAsOpaqueString() throws IOException {
        ServiceConfig svc = parser.parse(fixture("server-vault-refs.xml")).getServices().get(0);
        ConnectorConfig conn = svc.getConnectors().get(0);

        assertThat(conn.getProxyName()).isEqualTo("${VAULT::network::proxyHost::1}");
    }

    @Test
    void propertyPlaceholdersResolvedFromCatalinaProperties() throws IOException {
        Map<String, String> catProps = new HashMap<>();
        catProps.put("http.port", "9090");
        catProps.put("app.base", "custom-webapps");
        PropertyResolver resolver = new PropertyResolver(
                Collections.emptyMap(), catProps, Collections.emptyMap());
        ServerXmlParser resolverParser = new ServerXmlParser(resolver);

        ServiceConfig svc = resolverParser.parse(fixture("server-property-refs.xml"))
                .getServices().get(0);

        ConnectorConfig conn = svc.getConnectors().get(0);
        assertThat(conn.getPort()).isEqualTo(9090);

        HostConfig host = svc.getEngine().getHosts().get(0);
        assertThat(host.getAppBase().getValue()).isEqualTo("custom-webapps");
        assertThat(host.getAppBase().isExplicit()).isTrue();
    }

    @Test
    void malformedXmlThrowsIoException(@TempDir Path tmpDir) throws IOException {
        Path bad = tmpDir.resolve("bad.xml");
        Files.writeString(bad, "<Server port=\"8005\"><unclosed>");

        assertThatThrownBy(() -> parser.parse(bad))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("bad.xml");
    }
}
