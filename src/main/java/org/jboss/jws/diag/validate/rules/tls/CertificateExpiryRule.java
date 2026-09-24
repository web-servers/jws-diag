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
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * TLS-002: a certificate in a configured keystore has expired.
 *
 * <p>Keystores come from {@link KeystoreLocator}, so this covers Certificate, SSLHostConfig
 * and the older Connector attributes alike, and uses Tomcat's default password when none is
 * configured. A path whose placeholders cannot be expanded is skipped.
 */
public class CertificateExpiryRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        List<Finding> findings = new ArrayList<>();

        for (KeystoreLocator.Keystore keystore : KeystoreLocator.locate(ctx)) {
            if (!keystore.isResolved()) {
                continue;
            }
            try {
                KeyStore keyStore = KeyStore.getInstance(keystore.type);
                try (var is = Files.newInputStream(keystore.path)) {
                    keyStore.load(is, keystore.password.toCharArray());
                }

                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    Certificate cert = keyStore.getCertificate(alias);

                    if (cert instanceof X509Certificate) {
                        Date expiryDate = ((X509Certificate) cert).getNotAfter();
                        if (expiryDate.before(new Date())) {
                            findings.add(Finding.builder()
                                    .ruleId(RuleId.TLS_002)
                                    .category("TLS")
                                    .severity(Severity.ERROR)
                                    .summary("Certificate Expiry")
                                    .detail("Certificate in " + keystore.declaredFile
                                            + " expired on " + expiryDate)
                                    .file(keystore.declaredFile)
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
                        .detail("Could not load keystore " + keystore.declaredFile + ": " + e.getMessage())
                        .file(keystore.declaredFile)
                        .fix("Verify the keystore file exists, the path is correct, and the password is valid")
                        .build());
            }
        }

        return findings;
    }
}
