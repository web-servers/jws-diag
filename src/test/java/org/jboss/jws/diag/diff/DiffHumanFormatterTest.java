package org.jboss.jws.diag.diff;

import org.jboss.jws.diag.diff.formatter.DiffHumanFormatter;
import org.jboss.jws.diag.diff.model.ChangeType;
import org.jboss.jws.diag.diff.model.DiffEntry;
import org.jboss.jws.diag.diff.model.DiffReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiffHumanFormatterTest {

    private static final Path LEFT = Path.of("/opt/tomcat-a");
    private static final Path RIGHT = Path.of("/opt/tomcat-b");
    private final DiffHumanFormatter formatter = new DiffHumanFormatter();

    private DiffReport report(List<DiffEntry> entries) {
        return new DiffReport(LEFT, RIGHT, entries);
    }

    @Test
    void noDifferences_showsNoDifferencesMessage() {
        String out = formatter.format(report(Collections.emptyList()));

        assertThat(out).contains("No differences found.");
        assertThat(out).contains(LEFT.toString().replace('\\', '/'));
        assertThat(out).contains(RIGHT.toString().replace('\\', '/'));
    }

    @Test
    void changedEntry_showsArrowBetweenValues() {
        DiffEntry e = new DiffEntry("server.shutdownPort", ChangeType.CHANGED, "8005", "8006");
        String out = formatter.format(report(List.of(e)));

        assertThat(out).contains("8005");
        assertThat(out).contains("8006");
        assertThat(out).contains("→");
        assertThat(out).contains("shutdownPort");
    }

    @Test
    void addedEntry_showsAddedLabel() {
        DiffEntry e = new DiffEntry("services[Catalina].connectors[8443]",
                ChangeType.ADDED, null, "port 8443");
        String out = formatter.format(report(List.of(e)));

        assertThat(out).contains("+");
        assertThat(out).contains("added in right");
    }

    @Test
    void removedEntry_showsRemovedLabel() {
        DiffEntry e = new DiffEntry("services[Catalina].connectors[8443]",
                ChangeType.REMOVED, "port 8443", null);
        String out = formatter.format(report(List.of(e)));

        assertThat(out).contains("-");
        assertThat(out).contains("only in left");
    }

    @Test
    void changeCountShownInHeader() {
        List<DiffEntry> entries = List.of(
                new DiffEntry("server.shutdownPort", ChangeType.CHANGED, "8005", "8006"),
                new DiffEntry("services[Catalina].connectors[8080].maxThreads",
                        ChangeType.CHANGED, "200 (default)", "150 (explicit)")
        );
        String out = formatter.format(report(entries));

        assertThat(out).contains("2 change(s) found");
    }

    @Test
    void groupingBySection_relatedEntriesUnderSameSection() {
        List<DiffEntry> entries = List.of(
                new DiffEntry("services[Catalina].connectors[8080].maxThreads",
                        ChangeType.CHANGED, "200 (default)", "150 (explicit)"),
                new DiffEntry("services[Catalina].connectors[8080].connectionTimeout",
                        ChangeType.CHANGED, "20000 (default)", "60000 (explicit)")
        );
        String out = formatter.format(report(entries));

        // Both entries should appear under the same section header
        long sectionHeaderCount = out.lines()
                .filter(l -> l.contains("connectors[8080]") && !l.stripLeading().startsWith("~"))
                .count();
        assertThat(sectionHeaderCount).isEqualTo(1);
    }
}
