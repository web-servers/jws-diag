package org.jboss.jws.diag.validate.rules.security;

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

public class ShutdownPortConfigTest {

    private final ShutdownPortConfigRule rule = new ShutdownPortConfigRule();

    private Document parseFixture(String resourcePath) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        return db.parse(getClass().getResourceAsStream(resourcePath));
    }

    @Test
    void shouldPassWhenShutdownPortIsDisabled() throws Exception {
        Document serverXml = parseFixture("/fixtures/security/server-clean.xml");
        RuleContext ctx = new RuleContext(Path.of("/dummy"), serverXml, null);

        assertThat(rule.evaluate(ctx)).isEmpty();
    }

    @Test
    void shouldFlagWhenShutdownPortIsEnabled() throws Exception {
        Document serverXml = parseFixture("/fixtures/security/server-shutdown-port-enabled.xml");
        RuleContext ctx = new RuleContext(Path.of("/dummy"), serverXml, null);

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.SEC_003);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
    }

    @Test
    void shouldFlagWhenServerXmlIsNull() {
        RuleContext ctx = new RuleContext(Path.of("/dummy"), null, null);

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.SEC_003);
    }
}
