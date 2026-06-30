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

public class BadKeystorePathTest {

    private final BadKeystorePathRule rule = new BadKeystorePathRule();

    private Document parseFixture(String resourcePath) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(getClass().getResourceAsStream(resourcePath));
    }

    @Test
    void shouldPassWhenKeystorePathIsCorrect() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-cert-valid.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        assertThat(rule.evaluate(ctx)).isEmpty();
    }

    @Test
    void shouldFlagWhenKeystorePathIsIncorrect() throws Exception {
        Path catalinaBase = Path.of("src/test/resources/fixtures/tls/keystores");
        Document serverXml = parseFixture("/fixtures/tls/server-bad-keystore-path.xml");
        RuleContext ctx = new RuleContext(catalinaBase, serverXml, null, "testuser");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.TLS_005);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(findings.get(0).getDetail()).contains("Keystore file does not exist");
    }

    @Test
    void shouldPassWhenServerXmlIsNull() {
        RuleContext ctx = new RuleContext(Path.of("/dummy"), null, null, "testuser");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).isEmpty();
    }
}
