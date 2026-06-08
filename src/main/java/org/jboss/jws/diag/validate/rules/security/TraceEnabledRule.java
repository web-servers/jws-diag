package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class TraceEnabledRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getServerXml();

        if (doc == null) {
            return List.of();
        }

        NodeList connectors = doc.getElementsByTagName("Connector");
        List<Finding> findings = new ArrayList<>();

        for (int i = 0; i < connectors.getLength(); i++) {
            String allowTrace = connectors.item(i).getAttributes()
                    .getNamedItem("allowTrace") != null
                    ? connectors.item(i).getAttributes().getNamedItem("allowTrace").getNodeValue() : "false";

            if ("true".equals(allowTrace)) {
                findings.add(Finding.builder()
                        .ruleId(RuleId.SEC_005)
                        .category("Security")
                        .severity(Severity.WARN)
                        .summary("HTTP TRACE Enabled")
                        .detail("Checks if the HTTP TRACE method is allowed, which can leave it open to tracing attacks")
                        .file("server.xml")
                        .fix("Set allowTrace=\"false\" on your active connectors")
                        .build());
            }
        }

        return findings;
    }
}
