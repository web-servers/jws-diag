package org.jboss.jws.diag.validate.output;

import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.model.Finding;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HumanReadableOutput {

    private static final List<Severity> SEVERITIES = List.of(
            Severity.ERROR, Severity.WARN, Severity.INFO
    );

    public void print(List<Finding> findings) {
        if (findings.isEmpty()) {
            System.out.println("No issues found.");
            return;
        }

        Map<Severity, List<Finding>> issues = findings.stream()
                .collect(Collectors.groupingBy(Finding::getSeverity));

        for (Severity sev : SEVERITIES) {
            List<Finding> issuesForSeverity = issues.get(sev);
            if (issuesForSeverity == null ||  issuesForSeverity.isEmpty()) continue;

            System.out.printf("%n--- %s %s%n", sev, "--".repeat(50 - sev.name().length()));

            for (Finding find : issuesForSeverity) {
                System.out.printf("  [%s] %s %s%n", find.getRuleId(), find.getCategory(), find.getSummary());

                if (find.getDetail() != null) {
                    System.out.printf("     Detail : %s%n", find.getDetail());
                }
                if (find.getFile() != null) {
                    System.out.printf("     File   : %s%n", find.getFile());
                }
                if(find.getFix() != null) {
                    System.out.printf("     Fix    : %s%n", find.getFix());
                }
                System.out.println();
            }
        }

        FindingSummary summary = new FindingSummary(findings);

        System.out.printf("Summary: %d error(s), %d warning(s), %d info(s)%n",
        summary.errors, summary.warnings, summary.infos);
    }
}
