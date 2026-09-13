package org.jboss.jws.diag.validate.model;

import java.nio.file.Path;
import java.util.List;

/**
 * Findings for one running instance in a {@code validate --all} run.
 */
public final class InstanceValidationResult {

    private final int pid;
    private final Path catalinaBase;
    private final List<Finding> findings;

    public InstanceValidationResult(int pid, Path catalinaBase, List<Finding> findings) {
        this.pid = pid;
        this.catalinaBase = catalinaBase;
        this.findings = List.copyOf(findings);
    }

    public int getPid() {
        return pid;
    }

    public Path getCatalinaBase() {
        return catalinaBase;
    }

    public List<Finding> getFindings() {
        return findings;
    }
}
