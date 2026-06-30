package org.jboss.jws.diag.validate.rules.tls;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CertificateExpiryTest {

    private final CertificateExpiryRule rule = new CertificateExpiryRule();

    private Document parseFixture(String resourcePath) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(getClass().getResourceAsStream(resourcePath));
    }

    @Test
    void shouldFlagWhenCertificateIsExpired() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-cert-expired.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.TLS_002);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(findings.get(0).getDetail()).contains("expired");
    }

    @Test
    void shouldPassWhenCertificateIsValid() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-cert-valid.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        assertThat(rule.evaluate(ctx)).isEmpty();
    }

    @Test
    void shouldFlagWhenMultipleCertificatesAreExpired() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-cert-expired-multi.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.TLS_002);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(findings.get(0).getDetail()).contains("expired");
    }

    @Test
    void shouldFlagWhenKeystoreFileDoesNotExist() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-cert-keystore-missing.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.TLS_002);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(findings.get(0).getDetail()).contains("Could not load keystore");
    }

    @Test
    void shouldPassWhenPkcs12CertificateIsValid() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-cert-valid-pkcs12.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        assertThat(rule.evaluate(ctx)).isEmpty();
    }

    @Test
    void shouldFlagWhenPkcs12CertificateIsExpired() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-cert-expired-pkcs12.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.TLS_002);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(findings.get(0).getDetail()).contains("expired");
    }

    @Test
    void shouldFlagWhenMultiplePkcs12CertificatesAreExpired() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-cert-expired-multi-pkcs12.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(2);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.TLS_002);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(findings.get(0).getDetail()).contains("expired");
    }

    @Test
    void shouldPassWhenServerXmlIsNull() {
        RuleContext ctx = new RuleContext(Path.of("/dummy"), null, null, "testuser");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).isEmpty();
    }
}