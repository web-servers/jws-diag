package org.jboss.jws.diag.validate.rules.tls;

import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;

public class BadKeystorePathRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getServerXml();

        if (doc == null) {
            return List.of();
        }

        NodeList certificates = doc.getElementsByTagName("Certificate");
        List<Finding> findings = new ArrayList<>();

        for (int i = 0; i < certificates.getLength(); i++) {
            Node certNode = certificates.item(i);

            Node keystoreFileAttr = certNode.getAttributes().getNamedItem("certificateKeystoreFile");

            if (keystoreFileAttr == null) {
                continue;
            }

            String keystoreFile = keystoreFileAttr.getNodeValue();

            Path keystorePath = ctx.getCatalinaBase().resolve(keystoreFile);

            if (!Files.exists(keystorePath)) {
                findings.add(Finding.builder()
                        .ruleId(RuleId.TLS_005)
                        .category("TLS")
                        .severity(Severity.ERROR)
                        .summary("Bad Keystore Path")
                        .detail("Keystore file does not exist: " + keystoreFile)
                        .file("server.xml")
                        .fix("Correct the keystore file path attribute to point to a valid file")
                        .build());
            }
        }

        return findings;
    }
}
