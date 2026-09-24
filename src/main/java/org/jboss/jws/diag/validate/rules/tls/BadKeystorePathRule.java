package org.jboss.jws.diag.validate.rules.tls;

import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * TLS-005: a configured keystore file does not exist.
 *
 * <p>Keystores come from {@link KeystoreLocator}, so this covers Certificate, SSLHostConfig
 * and the older Connector attributes alike. A path whose placeholders cannot be expanded is
 * skipped: reporting it as missing would be a guess.
 */
public class BadKeystorePathRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        List<Finding> findings = new ArrayList<>();

        for (KeystoreLocator.Keystore keystore : KeystoreLocator.locate(ctx)) {
            if (!keystore.isResolved() || Files.exists(keystore.path)) {
                continue;
            }
            findings.add(Finding.builder()
                    .ruleId(RuleId.TLS_005)
                    .category("TLS")
                    .severity(Severity.ERROR)
                    .summary("Bad Keystore Path")
                    .detail("Keystore file does not exist: " + keystore.declaredFile)
                    .file("server.xml")
                    .fix("Correct the keystore file path attribute to point to a valid file")
                    .build());
        }

        return findings;
    }
}
