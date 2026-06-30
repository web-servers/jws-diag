package org.jboss.jws.diag.validate.rules.tls;

import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

public class CertificateExpiryRule implements Rule {

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
            Node keystorePasswordAttr = certNode.getAttributes().getNamedItem("certificateKeystorePassword");
            Node keystoreTypeAttr = certNode.getAttributes().getNamedItem("certificateKeystoreType");

            if (keystoreFileAttr == null) {
                continue;
            }

            String keystoreFile = keystoreFileAttr.getNodeValue();
            String keystorePassword = keystorePasswordAttr != null ?
                    keystorePasswordAttr.getNodeValue() : "";

            String certificateKeystoreType;
            if (keystoreTypeAttr != null) {
                certificateKeystoreType = keystoreTypeAttr.getNodeValue().toUpperCase();
            } else {
                String lower = keystoreFile.toLowerCase();
                certificateKeystoreType = (lower.endsWith(".p12") || lower.endsWith(".pfx")) ? "PKCS12" : "JKS";
            }

            Path keystorePath = ctx.getCatalinaBase().resolve(keystoreFile);

            try {
                KeyStore keyStore = KeyStore.getInstance(certificateKeystoreType);

                try (var is = Files.newInputStream(keystorePath)) {
                    keyStore.load(is, keystorePassword.toCharArray());
                }

                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate cert = keyStore.getCertificate(alias);

                    if (cert instanceof X509Certificate) {
                        X509Certificate x509Certificate = (X509Certificate) cert;
                        Date expiryDate = x509Certificate.getNotAfter();

                        if (expiryDate.before(new Date())) {
                            findings.add(Finding.builder()
                                    .ruleId(RuleId.TLS_002)
                                    .category("TLS")
                                    .severity(Severity.ERROR)
                                    .summary("Certificate Expiry")
                                    .detail("Certificate in " + keystoreFile + " expired on " + expiryDate)
                                    .file(keystoreFile)
                                    .fix("Renew and install a valid SSL/TLS certificate immediately")
                                    .build());
                        }
                    }
                }

            } catch (GeneralSecurityException | IOException e) {
                findings.add(Finding.builder()
                        .ruleId(RuleId.TLS_002)
                        .category("TLS")
                        .severity(Severity.ERROR)
                        .summary("Certificate Expiry")
                        .detail("Could not load keystore " + keystoreFile + ": " + e.getMessage())
                        .file(keystoreFile)
                        .fix("Verify the keystore file exists, the path is correct, and the password is valid")
                        .build());
            }
        }

        return findings;
    }
}
