package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;

import java.util.List;

public class ShutdownPortConfigRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        boolean violated = ctx.getServerXml() == null ||
                !"-1".equals(ctx.getServerXml().getDocumentElement().getAttribute("port"));

        if (violated) {
            return List.of(Finding.builder()
                    .ruleId(RuleId.SEC_003)
                    .category("Security")
                    .severity(Severity.ERROR)
                    .summary("Shutdown Port Configuration Check")
                    .detail("Inspects the <Server> element's port attribute to ensure it is set to \"-1\". It flags an issue if it is missing or set to a standard network port (like 8005).")
                    .file("server.xml")
                    .fix("Set the port attribute of the <Server> element to \"-1\" to disable network-based shutdown.")
                    .build());
        }
        return List.of();
    }
}
