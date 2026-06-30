package org.jboss.jws.diag.validate.rules.tls;

import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.List;
import java.util.ArrayList;

public class MissingSecureFlagRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getServerXml();

        if (doc == null) {
            return List.of();
        }

        NodeList connectors = doc.getElementsByTagName("Connector");
        List<Finding> findings = new ArrayList<>();

        for (int i = 0; i < connectors.getLength(); i++) {
            Node connector = connectors.item(i);

            Node sslEnabledAttr = connector.getAttributes().getNamedItem("SSLEnabled");
            Node secureAttr = connector.getAttributes().getNamedItem("secure");
            Node portAttr = connector.getAttributes().getNamedItem("port");

            boolean sslEnabled = sslEnabledAttr != null && "true".equalsIgnoreCase(sslEnabledAttr.getNodeValue());
            boolean securePresent = secureAttr != null && "true".equalsIgnoreCase(secureAttr.getNodeValue());
            String port = portAttr != null ? portAttr.getNodeValue() : "unknown";

            if (sslEnabled && !securePresent) {
                findings.add(Finding.builder()
                        .ruleId(RuleId.TLS_003)
                        .category("TLS")
                        .severity(Severity.WARN)
                        .summary("Missing Secure Flag")
                        .detail("Connector on port " + port + " has SSLEnabled=\"true\" but is missing secure=\"true\"")
                        .file("server.xml")
                        .fix("Add secure=\"true\" to the connector configuration fields")
                        .build());
            }
        }

        return findings;
    }
}
