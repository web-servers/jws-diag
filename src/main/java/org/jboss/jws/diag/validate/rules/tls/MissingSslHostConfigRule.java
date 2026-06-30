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

public class MissingSslHostConfigRule implements Rule {

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
            Node portAttr = connector.getAttributes().getNamedItem("port");

            boolean sslEnabled = sslEnabledAttr != null && "true".equalsIgnoreCase(sslEnabledAttr.getNodeValue());
            boolean hasSSLHostConfig = false;
            String port = portAttr != null ? portAttr.getNodeValue() : "unknown";

            NodeList configs = connector.getChildNodes();

            for (int j = 0; j < configs.getLength(); j++) {
                if ("SSLHostConfig".equals(configs.item(j).getNodeName())) {
                    hasSSLHostConfig = true;
                    break;
                }
            }

            if (sslEnabled && !hasSSLHostConfig) {
                findings.add(Finding.builder()
                        .ruleId(RuleId.TLS_004)
                        .category("TLS")
                        .severity(Severity.WARN)
                        .summary("Missing SSLHostConfig")
                        .detail("Connector on port " + port + " does not have SSLHostConfig")
                        .file("server.xml")
                        .fix("Move inline SSL configuration attributes into a defined <SSLHostConfig> block")
                        .build());
            }
        }

        return findings;
    }
}
