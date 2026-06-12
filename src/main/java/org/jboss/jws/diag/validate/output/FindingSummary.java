package org.jboss.jws.diag.validate.output;

import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.model.Finding;

import java.util.List;

public class FindingSummary {

    private final long errors;
    private final long warnings;
    private final long info;

    public FindingSummary(List<Finding> findings) {
        this.errors = findings.stream().filter(f -> f.getSeverity() == Severity.ERROR).count();
        this.warnings = findings.stream().filter(f -> f.getSeverity() == Severity.WARN).count();
        this.info = findings.stream().filter(f -> f.getSeverity() == Severity.INFO).count();
    }

    public long getErrors() {
        return errors;
    }

    public long getWarnings() {
        return warnings;
    }

    public long getInfo() {
        return info;
    }
}
