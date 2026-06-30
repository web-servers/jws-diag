package org.jboss.jws.diag.validate;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.validate.output.HumanReadableOutput;
import org.jboss.jws.diag.validate.output.JsonOutput;
import org.jboss.jws.diag.validate.rules.security.RootUserCheckRule;
import org.jboss.jws.diag.validate.rules.security.UserDefaultCredentialsRule;
import org.jboss.jws.diag.validate.rules.security.ShutdownPortConfigRule;
import org.jboss.jws.diag.validate.rules.security.ErrorValveRule;
import org.jboss.jws.diag.validate.rules.security.TraceEnabledRule;
import org.jboss.jws.diag.validate.rules.security.LocalhostBindingRule;
import org.jboss.jws.diag.validate.rules.tls.DeprecatedProtocolsRule;
import org.jboss.jws.diag.validate.rules.tls.CertificateExpiryRule;
import org.jboss.jws.diag.validate.rules.tls.BadKeystorePathRule;
import org.jboss.jws.diag.validate.rules.tls.MissingSecureFlagRule;
import org.jboss.jws.diag.validate.rules.tls.MissingSslHostConfigRule;
import org.jboss.jws.diag.validate.rules.tls.WeakCipherSuitesRule;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Command(name = "validate",
        description = "Run diagnostic rules against configuration and report findings (INFO/WARN/ERROR)",
        mixinStandardHelpOptions = true)
public class ValidateCommand implements Runnable {

    @CommandLine.Parameters(index = "0", description = "Path to CATALINA_BASE directory")
    private Path catalinaBase;

    @Mixin
    private OutputFormatMixin outputFormat;

    private final List<Rule> rules = List.of(
            new RootUserCheckRule(),
            new UserDefaultCredentialsRule(),
            new ShutdownPortConfigRule(),
            new ErrorValveRule(),
            new TraceEnabledRule(),
            new LocalhostBindingRule(),
            new DeprecatedProtocolsRule(),
            new CertificateExpiryRule(),
            new BadKeystorePathRule(),
            new MissingSecureFlagRule(),
            new MissingSslHostConfigRule(),
            new WeakCipherSuitesRule()
    );

    @Override
    public void run() {
        RuleContext ctx = RuleContext.fromDisk(catalinaBase);

        List<Finding> findings = new ArrayList<>();
        for (Rule rule : rules) {
            findings.addAll(rule.evaluate(ctx));
        }

        int exitCode = determineExitCode(findings);

        switch (outputFormat.getFormat()) {
            case HUMAN:
                new HumanReadableOutput().print(findings);
                break;
            case JSON:
                new JsonOutput().print(findings, exitCode);
                break;
        }

        System.exit(exitCode);
    }

    public int determineExitCode(List<Finding> findings) {
        int highestCode = ExitCodes.OK;

        for (Finding finding : findings) {
            if (finding.getSeverity() == Severity.ERROR) {
                highestCode = ExitCodes.ERRORS;
            } else if (finding.getSeverity() == Severity.WARN && highestCode < ExitCodes.ERRORS) {
                highestCode = ExitCodes.WARNINGS;
            }
        }

        return highestCode;
    }
}
