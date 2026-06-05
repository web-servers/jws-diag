package org.jboss.jws.diag.validate.rules.security;

import org.jboss.jws.diag.validate.Rule;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;

import java.util.List;

public class RootUserCheckRule implements Rule {

    @Override
    public List<Finding> evaluate(RuleContext ctx) {
        String username = System.getProperty("user.name");

        if ("root".equals(username)) {
            return List.of(Finding.builder()
                    .ruleId(RuleId.SEC_001)
                    .category("Security")
                    .severity(Severity.ERROR)
                    .summary("Root User Check")
                    .detail("Checks if the Tomcat process is running as root (UID 0)")
                    .file("Process State")
                    .fix("Run Tomcat as a dedicated, non-root system user")
                    .build());
        }

        return List.of();
    }
}
