package org.jboss.jws.diag.diff.formatter;

import org.jboss.jws.diag.diff.model.InstanceDiffResult;
import org.jboss.jws.diag.diff.model.MultiDiffReport;

/**
 * Renders a {@link MultiDiffReport} as human-readable text.
 *
 * <p>Example:
 * <pre>
 * Auto-discovered 3 instance(s). Reference: PID 1234  /opt/jws-6.0/standalone
 *
 * Instance PID 5678  /opt/jws-5.7/standalone
 * ────────────────────────────────────────────────────────────────────────────────
 * 3 change(s) found:
 *
 * services[Catalina].connectors[8443]
 *   ~ protocol   HTTP/1.1 (default)  →  org.apache.coyote.http11.Http11NioProtocol (explicit)
 *
 * Instance PID 9012  /opt/jws-6.1/standalone
 * ────────────────────────────────────────────────────────────────────────────────
 * No differences found.
 * </pre>
 */
public final class MultiDiffHumanFormatter {

    private static final String RULE = "─".repeat(80);
    private static final DiffHumanFormatter SINGLE = new DiffHumanFormatter();

    public String format(MultiDiffReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Auto-discovered %d instance(s). Reference: PID %d  %s%n",
                report.getInstanceCount(),
                report.getReferencePid(),
                report.getReferenceBase().toString().replace('\\', '/')));

        for (InstanceDiffResult result : report.getComparisons()) {
            sb.append('\n');
            sb.append(String.format("Instance PID %d  %s%n", result.getPid(), result.getCatalinaBase()));
            sb.append(RULE).append('\n');
            sb.append(SINGLE.formatBody(result.getDiff()));
        }
        return sb.toString();
    }
}
