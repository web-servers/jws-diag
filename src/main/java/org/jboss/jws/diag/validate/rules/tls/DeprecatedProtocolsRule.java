package org.jboss.jws.diag.validate.rules.tls;

import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeprecatedProtocolsRule implements Rule {

    private static final Set<String> DEPRECATED_PROTOCOLS = Set.of(
            "SSLv2", "SSLv3", "TLSv1", "TLSv1.1"
    );

    private static final List<String> ATTRIBUTE_NAMES = List.of(
            "sslEnabledProtocols", "protocols"
    );

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getServerXml();

        if (doc == null) {
            return List.of();
        }

        NodeList sslHostConfigs = doc.getElementsByTagName("SSLHostConfig");
        List<Finding> findings = new ArrayList<>();

        for (int i = 0; i < sslHostConfigs.getLength(); i++) {
            Node sslHostConfig = sslHostConfigs.item(i);
            List<String> foundDeprecated = new ArrayList<>();

            for (String attributeName : ATTRIBUTE_NAMES) {
                Node protocolsAttribute = sslHostConfig.getAttributes().getNamedItem(attributeName);

                if (protocolsAttribute == null) {
                    continue;
                }

                String[] protocols = protocolsAttribute.getNodeValue().split(",");

                for (String protocol : protocols) {
                    String trimmed = protocol.trim();
                    if (DEPRECATED_PROTOCOLS.contains(trimmed) && !foundDeprecated.contains(trimmed)) {
                        foundDeprecated.add(trimmed);
                    }
                }
            }

            if (!foundDeprecated.isEmpty()) {
                findings.add(Finding.builder()
                        .ruleId(RuleId.TLS_001)
                        .category("TLS")
                        .severity(Severity.WARN)
                        .summary("Deprecated Protocols")
                        .detail("Obsolete TLS versions detected: " + String.join(", ", foundDeprecated))
                        .file("server.xml")
                        .fix("Update configuration to allow only modern TLSv1.2 or TLSv1.3")
                        .build()
                );
            }
        }

        return findings;
    }
}
