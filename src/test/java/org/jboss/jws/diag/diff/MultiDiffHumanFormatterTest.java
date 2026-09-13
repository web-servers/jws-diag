package org.jboss.jws.diag.diff;

import org.jboss.jws.diag.diff.formatter.MultiDiffHumanFormatter;
import org.jboss.jws.diag.diff.model.ChangeType;
import org.jboss.jws.diag.diff.model.DiffEntry;
import org.jboss.jws.diag.diff.model.DiffReport;
import org.jboss.jws.diag.diff.model.InstanceDiffResult;
import org.jboss.jws.diag.diff.model.MultiDiffReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultiDiffHumanFormatterTest {

    private static final Path REF_BASE = Path.of("/opt/jws-6.0/standalone");
    private static final Path INST_BASE = Path.of("/opt/jws-5.7/standalone");
    private final MultiDiffHumanFormatter formatter = new MultiDiffHumanFormatter();

    @Test
    void header_showsInstanceCountAndReferencePid() {
        InstanceDiffResult result = identicalResult(200, INST_BASE);
        MultiDiffReport report = multiReport(List.of(result));

        String out = formatter.format(report);

        assertThat(out).contains("Auto-discovered 2 instance(s)");
        assertThat(out).contains("PID 100");
        assertThat(out).contains(REF_BASE.toString().replace('\\', '/'));
    }

    @Test
    void instanceSection_showsPidAndBase() {
        InstanceDiffResult result = identicalResult(200, INST_BASE);
        MultiDiffReport report = multiReport(List.of(result));

        String out = formatter.format(report);

        assertThat(out).contains("Instance PID 200");
        assertThat(out).contains(INST_BASE.toString().replace('\\', '/'));
    }

    @Test
    void identicalInstance_showsNoDifferencesMessage() {
        InstanceDiffResult result = identicalResult(200, INST_BASE);
        MultiDiffReport report = multiReport(List.of(result));

        String out = formatter.format(report);

        assertThat(out).contains("No differences found.");
    }

    @Test
    void instanceWithChanges_showsChangeCount() {
        DiffEntry entry = new DiffEntry("server.shutdownPort", ChangeType.CHANGED, "8005", "8006");
        InstanceDiffResult result = resultWithEntries(200, INST_BASE, List.of(entry));
        MultiDiffReport report = multiReport(List.of(result));

        String out = formatter.format(report);

        assertThat(out).contains("1 change(s) found");
        assertThat(out).contains("shutdownPort");
        assertThat(out).contains("8005");
        assertThat(out).contains("8006");
    }

    @Test
    void multipleInstances_eachSectionPresent() {
        Path base2 = Path.of("/opt/jws-6.1/standalone");
        InstanceDiffResult r1 = identicalResult(200, INST_BASE);
        DiffEntry entry = new DiffEntry("server.shutdownPort", ChangeType.CHANGED, "8005", "9005");
        InstanceDiffResult r2 = resultWithEntries(300, base2, List.of(entry));
        MultiDiffReport report = new MultiDiffReport(100, REF_BASE, 3, List.of(r1, r2));

        String out = formatter.format(report);

        assertThat(out).contains("Auto-discovered 3 instance(s)");
        assertThat(out).contains("Instance PID 200");
        assertThat(out).contains("Instance PID 300");
        assertThat(out).contains("No differences found.");
        assertThat(out).contains("1 change(s) found");
    }

    @Test
    void singleDiffHeader_notRepeatedInOutput() {
        InstanceDiffResult result = identicalResult(200, INST_BASE);
        MultiDiffReport report = multiReport(List.of(result));

        String out = formatter.format(report);

        // The "Diff  left :" header from DiffHumanFormatter must not appear
        assertThat(out).doesNotContain("Diff  left");
        assertThat(out).doesNotContain("right:");
    }

    @Test
    void backslashSeparatedPaths_renderWithForwardSlashes() {
        // Backslashes stay literal in a Path on Linux too, so this exercises the
        // Windows rendering on every platform.
        Path windowsRef = Path.of("\\opt\\jws-6.0\\standalone");
        Path windowsInst = Path.of("\\opt\\jws-5.7\\standalone");
        DiffReport diff = new DiffReport(windowsRef, windowsInst, Collections.emptyList());
        MultiDiffReport report = new MultiDiffReport(100, windowsRef, 2,
                List.of(new InstanceDiffResult(200, windowsInst, diff)));

        String out = formatter.format(report);

        assertThat(out).contains("/opt/jws-6.0/standalone");
        assertThat(out).contains("/opt/jws-5.7/standalone");
        assertThat(out).doesNotContain("\\");
    }

    private MultiDiffReport multiReport(List<InstanceDiffResult> comparisons) {
        return new MultiDiffReport(100, REF_BASE, comparisons.size() + 1, comparisons);
    }

    private InstanceDiffResult identicalResult(int pid, Path base) {
        DiffReport diff = new DiffReport(REF_BASE, base, Collections.emptyList());
        return new InstanceDiffResult(pid, base, diff);
    }

    private InstanceDiffResult resultWithEntries(int pid, Path base, List<DiffEntry> entries) {
        DiffReport diff = new DiffReport(REF_BASE, base, entries);
        return new InstanceDiffResult(pid, base, diff);
    }
}
