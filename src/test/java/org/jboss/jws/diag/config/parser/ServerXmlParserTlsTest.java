package org.jboss.jws.diag.config.parser;

import org.jboss.jws.diag.config.model.CertificateConfig;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.RealmConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.model.SslHostConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServerXmlParserTlsTest {

    private ServerXmlParser parser;

    @BeforeEach
    void setUp() {
        parser = new ServerXmlParser(new PropertyResolver(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()));
    }

    private Path fixture(String name) throws URISyntaxException {
        return Paths.get(getClass().getClassLoader()
                .getResource("fixtures/config/" + name).toURI());
    }

    // --- TLS connector ---

    @Test
    void tlsConnectorHasSslHostConfig() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        ConnectorConfig tls = cfg.getServices().get(0).getConnectors().stream()
                .filter(c -> c.getPort() == 8443).findFirst().orElseThrow();
        assertThat(tls.getSsl()).isNotNull();
    }

    @Test
    void sslHostConfigProtocolsParsed() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        SslHostConfig ssl = tlsConnector(cfg).getSsl();
        assertThat(ssl.getProtocols()).isEqualTo("TLSv1.2,TLSv1.3");
    }

    @Test
    void sslHostConfigCertificateVerificationParsed() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        SslHostConfig ssl = tlsConnector(cfg).getSsl();
        assertThat(ssl.getCertificateVerification()).isEqualTo("none");
    }

    @Test
    void certificateKeystoreFileParsed() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        CertificateConfig cert = firstCert(cfg);
        assertThat(cert.getKeystoreFile()).isEqualTo("conf/localhost-rsa.jks");
    }

    @Test
    void certificateTypeParsed() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        CertificateConfig cert = firstCert(cfg);
        assertThat(cert.getType()).isEqualTo("RSA");
    }

    @Test
    void keystoreTypeExplicitFromXml() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        CertificateConfig cert = firstCert(cfg);
        assertThat(cert.getKeystoreType().getValue()).isEqualTo("JKS");
        assertThat(cert.getKeystoreType().isExplicit()).isTrue();
    }

    @Test
    void keystorePasswordIsRedacted() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        CertificateConfig cert = firstCert(cfg);
        assertThat(cert.getKeystorePass()).isEqualTo("***REDACTED***");
    }

    @Test
    void keystorePasswordNotLeakedInOutput() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        // Raw password "s3cretP@ss" must not appear anywhere
        assertThat(cfg.toString()).doesNotContain("s3cretP@ss");
        CertificateConfig cert = firstCert(cfg);
        assertThat(cert.getKeystorePass()).doesNotContain("s3cretP@ss");
    }

    // --- VAULT token in keystorePassword ---

    @Test
    void vaultTokenPreservedInKeystorePassword() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-vault-tls.xml"));
        CertificateConfig cert = firstCert(cfg);
        assertThat(cert.getKeystorePass()).isEqualTo("${VAULT::ssl::keystorePassword::1}");
    }

    @Test
    void keystoreTypeDefaultedWhenAbsent() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-vault-tls.xml"));
        CertificateConfig cert = firstCert(cfg);
        assertThat(cert.getKeystoreType().getValue()).isEqualTo("JKS");
        assertThat(cert.getKeystoreType().isExplicit()).isFalse();
    }

    // --- Realm ---

    @Test
    void engineRealmParsed() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        RealmConfig realm = cfg.getServices().get(0).getEngine().getRealm();
        assertThat(realm).isNotNull();
        assertThat(realm.getClassName())
                .isEqualTo("org.apache.catalina.realm.LockOutRealm");
    }

    @Test
    void nestedRealmParsed() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-full-tls.xml"));
        RealmConfig realm = cfg.getServices().get(0).getEngine().getRealm();
        assertThat(realm.getNestedRealms()).hasSize(1);
        assertThat(realm.getNestedRealms().get(0).getClassName())
                .isEqualTo("org.apache.catalina.realm.UserDatabaseRealm");
    }

    @Test
    void noRealmReturnsNull() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-valid-basic.xml"));
        RealmConfig realm = cfg.getServices().get(0).getEngine().getRealm();
        assertThat(realm).isNull();
    }

    // --- Multi-service ---

    @Test
    void multiServiceBothParsed() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-multi-service.xml"));
        assertThat(cfg.getServices()).hasSize(2);
    }

    @Test
    void multiServiceNamesCorrect() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-multi-service.xml"));
        List<String> names = cfg.getServices().stream()
                .map(s -> s.getName())
                .collect(java.util.stream.Collectors.toList());
        assertThat(names).containsExactly("Catalina", "Admin");
    }

    @Test
    void multiServiceSecondConnectorPort() throws Exception {
        ServerConfig cfg = parser.parse(fixture("server-multi-service.xml"));
        assertThat(cfg.getServices().get(1).getConnectors().get(0).getPort()).isEqualTo(9080);
    }

    // --- Helpers ---

    private ConnectorConfig tlsConnector(ServerConfig cfg) {
        return cfg.getServices().get(0).getConnectors().stream()
                .filter(c -> c.getPort() == 8443).findFirst().orElseThrow();
    }

    private CertificateConfig firstCert(ServerConfig cfg) {
        SslHostConfig ssl = tlsConnector(cfg).getSsl();
        assertThat(ssl).isNotNull();
        assertThat(ssl.getCertificates()).isNotEmpty();
        return ssl.getCertificates().get(0);
    }
}
