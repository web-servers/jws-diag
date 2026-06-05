package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.util.List;

public class LocalhostBindingRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getServerXml();

        if (doc == null) {
            return List.of(Finding.builder()
                    .ruleId(RuleId.SEC_006)
                    .category("Security")
                    .severity(Severity.INFO)
                    .summary("Localhost Binding")
                    .detail("Checks if the connector address attribute is restricted to localhost (127.0.0.1)")
                    .file("server.xml")
                    .fix("If you want the server accessible to the public, change address to 0.0.0.0")
                    .build());
        }

        NodeList connectors = doc.getElementsByTagName("Connector");

        for (int i = 0; i < connectors.getLength(); i++) {
            String address = connectors.item(i).getAttributes()
                    .getNamedItem("address") != null
                    ? connectors.item(i).getAttributes().getNamedItem("address").getNodeValue() : "0.0.0.0";

            if (!"127.0.0.1".equals(address)) {
                return List.of(Finding.builder()
                        .ruleId(RuleId.SEC_006)
                        .category("Security")
                        .severity(Severity.INFO)
                        .summary("Localhost Binding")
                        .detail("Checks if the connector address attribute is restricted to localhost (127.0.0.1)")
                        .file("server.xml")
                        .fix("If you want the server accessible to the public, change address to 0.0.0.0")
                        .build());
            }
        }

        return List.of();
    }
}
