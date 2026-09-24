package org.jboss.jws.diag.validate.rules.tls;

import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;

import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class CertificateChainVerificationRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        List<Finding> findings = new ArrayList<>();

        for (KeystoreLocator.Keystore keystore : KeystoreLocator.locate(ctx)) {
            if (!keystore.isResolved() || !Files.exists(keystore.path)) {
                continue;
            }
            try {
                KeyStore keyStore = KeyStore.getInstance(keystore.type);

                try (var inputStream = Files.newInputStream(keystore.path)) {
                    keyStore.load(inputStream, keystore.password.toCharArray());
                }

                Enumeration<String> aliases = keyStore.aliases();

                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();

                    if (!keyStore.isKeyEntry(alias)) {
                        continue;
                    }

                    Certificate[] chain = keyStore.getCertificateChain(alias);

                    if (chain == null || chain.length == 0) {
                        findings.add(Finding.builder()
                                .ruleId(RuleId.TLS_007)
                                .category("TLS")
                                .severity(Severity.WARN)
                                .summary("Certificate Chain Verification")
                                .detail("Certificate for alias '" + alias + "' in " + keystore.declaredFile
                                        + " does not contain a certificate chain.")
                                .file(keystore.declaredFile)
                                .fix("Bundle the required intermediate CA certificate(s) "
                                        + "into the keystore alongside the end-entity certificate.")
                                .build());

                        continue;
                    }

                    boolean chainValid = true;

                    for (int j = 0; j < chain.length - 1; j++) {
                        if (!(chain[j] instanceof X509Certificate)
                                || !(chain[j + 1] instanceof X509Certificate)) {
                            chainValid = false;
                            break;
                        }

                        X509Certificate current = (X509Certificate) chain[j];
                        X509Certificate issuer = (X509Certificate) chain[j + 1];

                        if (!current.getIssuerX500Principal().equals(issuer.getSubjectX500Principal())) {
                            chainValid = false;
                            break;
                        }
                    }

                    X509Certificate leaf = chain[0] instanceof X509Certificate
                            ? (X509Certificate) chain[0] : null;

                    boolean selfSigned = leaf != null && leaf.getSubjectX500Principal()
                            .equals(leaf.getIssuerX500Principal());

                    if (!chainValid || (chain.length == 1 && !selfSigned)) {
                        findings.add(Finding.builder()
                                .ruleId(RuleId.TLS_007)
                                .category("TLS")
                                .severity(Severity.WARN)
                                .summary("Certificate Chain Verification")
                                .detail("Certificate chain for alias '" + alias
                                        + "' in " + keystore.declaredFile
                                        + " is incomplete or contains a missing intermediate certificate.")
                                .file(keystore.declaredFile)
                                .fix("Bundle the required intermediate CA certificate(s) "
                                        + "into the keystore alongside the end-entity certificate.")
                                .build());
                    }
                }

            } catch (GeneralSecurityException | IOException e) {
                // CertificateExpiryRule already report keystore loading failures.
            }
        }

        return findings;
    }
}