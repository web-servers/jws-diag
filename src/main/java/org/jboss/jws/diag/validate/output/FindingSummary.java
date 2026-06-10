package org.jboss.jws.diag.validate.output;

import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.model.Finding;

import java.util.List;

public class FindingSummary {

    public final long errors;
    public final long warnings;
    public final long infos;

    public FindingSummary(List<Finding> findings) {
        this.errors = findings.stream().filter(f -> f.getSeverity() == Severity.ERROR).count();
        this.warnings = findings.stream().filter(f -> f.getSeverity() == Severity.WARN).count();
        this.infos = findings.stream().filter(f -> f.getSeverity() == Severity.INFO).count();
    }
}
