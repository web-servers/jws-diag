package org.jboss.jws.diag.diff.formatter;

import org.jboss.jws.diag.diff.model.ChangeType;
import org.jboss.jws.diag.diff.model.DiffEntry;
import org.jboss.jws.diag.diff.model.DiffReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a {@link DiffReport} as human-readable text grouped by path prefix.
 *
 * <p>Example:
 * <pre>
 * Diff  left : /opt/tomcat-a
 *       right: /opt/tomcat-b
 *
 * server
 *   ~ shutdownPort          8005  →  8006
 *
 * services[Catalina].connectors[8080]
 *   ~ maxThreads            200 (default)  →  150 (explicit)
 *
 * services[Catalina].connectors[8443]
 *   + ADDED
 *
 * No differences found.
 * </pre>
 */
public final class DiffHumanFormatter {

    public String format(DiffReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Diff  left : %s%n", report.getLeft().toString().replace('\\', '/')));
        sb.append(String.format("      right: %s%n", report.getRight().toString().replace('\\', '/')));
        sb.append(formatBody(report));
        return sb.toString();
    }

    /**
     * Renders only the diff body (change list or "No differences found"),
     * without the two-line left/right header. Intended for composition by
     * {@code MultiDiffHumanFormatter}.
     */
    public String formatBody(DiffReport report) {
        StringBuilder sb = new StringBuilder();

        if (!report.hasDifferences()) {
            sb.append("\nNo differences found.\n");
            return sb.toString();
        }

        sb.append(String.format("%n%d change(s) found:%n", report.getChangeCount()));

        // Group entries by their section (everything up to the last '.')
        Map<String, List<DiffEntry>> groups = new LinkedHashMap<>();
        for (DiffEntry e : report.getEntries()) {
            String section = sectionOf(e.getPath());
            groups.computeIfAbsent(section, k -> new ArrayList<>()).add(e);
        }

        for (Map.Entry<String, List<DiffEntry>> g : groups.entrySet()) {
            sb.append('\n').append(g.getKey()).append('\n');
            for (DiffEntry e : g.getValue()) {
                String field = fieldOf(e.getPath());
                switch (e.getType()) {
                    case ADDED:
                        sb.append(String.format("  + %-28s (added in right)%n", field));
                        break;
                    case REMOVED:
                        sb.append(String.format("  - %-28s (only in left)%n", field));
                        break;
                    case CHANGED:
                        sb.append(String.format("  ~ %-28s %s  →  %s%n",
                                field,
                                nullSafe(e.getLeftValue()),
                                nullSafe(e.getRightValue())));
                        break;
                }
            }
        }
        return sb.toString();
    }

    private static String sectionOf(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }

    private static String fieldOf(String path) {
        int dot = path.lastIndexOf('.');
        // For ADDED/REMOVED whole-section entries (e.g. connectors[8443]) the path IS the section
        return dot < 0 ? path : path.substring(dot + 1);
    }

    private static String nullSafe(String s) {
        return s == null ? "(absent)" : s;
    }
}
