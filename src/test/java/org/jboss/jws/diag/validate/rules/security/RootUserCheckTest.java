package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RootUserCheckTest {

    private final RootUserCheckRule rule = new RootUserCheckRule();

    @Test
    void shouldPassWhenNotRunningAsRoot() {
        RuleContext ctx = new RuleContext(Path.of("/dummy"), null, null, "tomcat");

        assertThat(rule.evaluate(ctx)).isEmpty();
    }

    @Test
    void shouldFlagWhenRunningAsRoot() {
        RuleContext ctx = new RuleContext(Path.of("/dummy"), null, null, "root");

        List<Finding> findings = rule.evaluate(ctx);

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.SEC_001);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.ERROR);
    }
}
