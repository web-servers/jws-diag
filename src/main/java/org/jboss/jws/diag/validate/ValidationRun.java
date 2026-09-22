package org.jboss.jws.diag.validate;

import org.jboss.jws.diag.validate.model.Finding;

import java.util.List;

/**
 * The outcome of running every rule against one installation.
 *
 * <p>When {@code server.xml} could not be read, the rules that depend on it produced
 * nothing, so the findings do not describe the installation. {@link #isComplete()} is
 * then false and {@link #getServerXmlProblem()} says why.
 */
public final class ValidationRun {

    private final List<Finding> findings;
    private final String serverXmlProblem;

    ValidationRun(List<Finding> findings, String serverXmlProblem) {
        this.findings = List.copyOf(findings);
        this.serverXmlProblem = serverXmlProblem;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    /** True when server.xml was read and every rule could be evaluated against it. */
    public boolean isComplete() {
        return serverXmlProblem == null;
    }

    /** Why server.xml could not be used, or null when it was read successfully. */
    public String getServerXmlProblem() {
        return serverXmlProblem;
    }
}
