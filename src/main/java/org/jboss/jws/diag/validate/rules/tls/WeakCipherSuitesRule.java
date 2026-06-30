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

public class WeakCipherSuitesRule implements Rule{

    private static final List<String> WEAK_KEYWORDS = List.of(
            "RC4", "DES", "NULL", "EXPORT", "EXP", "MD5", "anon"
    );

    private boolean isWeakCipher(String cipher) {
        String upper = cipher.trim().toUpperCase();

        for (String keyword : WEAK_KEYWORDS) {
            if (upper.contains(keyword)) {
                return true;
            }
        }

        return upper.contains("SHA") && !upper.matches(".*SHA\\d{3,}.*");
    }

    private boolean isCbcCipher(String cipher) {
        return cipher != null && cipher.trim().toUpperCase().contains("CBC");
    }

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        Document doc = ctx.getServerXml();

        if (doc == null) {
            return List.of();
        }

        NodeList sslHostConfigs = doc.getElementsByTagName("SSLHostConfig");
        List<Finding> findings = new ArrayList<>();

        for (int i = 0; i < sslHostConfigs.getLength(); i++) {
            Node sslHostConfigNode = sslHostConfigs.item(i);

            Node cipherAttr =  sslHostConfigNode.getAttributes().getNamedItem("ciphers");

            if (cipherAttr == null) {
                continue;
            }

            String[] cipherList = cipherAttr.getNodeValue().split(",");
            List<String> weakFound = new ArrayList<>();
            List<String> cbcFound = new ArrayList<>();

            for (String cipher : cipherList) {
                String trimmed =  cipher.trim();

                if (trimmed.isEmpty()) continue;

                if (isWeakCipher(trimmed)) {
                    weakFound.add(trimmed);
                } else if (isCbcCipher(trimmed)) {
                    cbcFound.add(trimmed);
                }
            }

            if (!weakFound.isEmpty()) {
                findings.add(Finding.builder()
                        .ruleId(RuleId.TLS_006)
                        .category("TLS")
                        .severity(Severity.WARN)
                        .summary("Weak Cipher Suites")
                        .detail("Weak ciphers detected: " + String.join(", ", weakFound))
                        .file("server.xml")
                        .fix("Configure the connector to use strong cipher suites like AES-GCM")
                        .build());
            }

            if (!cbcFound.isEmpty()) {
                findings.add(Finding.builder()
                        .ruleId(RuleId.TLS_006)
                        .category("TLS")
                        .severity(Severity.INFO)
                        .summary("Weak Cipher Suites")
                        .detail("CBC cipher suites detected: " + String.join(", ", cbcFound))
                        .file("server.xml")
                        .fix("Consider upgrading to GCM-based cipher suites like AES-GCM")
                        .build());
            }
        }

        return findings;
    }
}