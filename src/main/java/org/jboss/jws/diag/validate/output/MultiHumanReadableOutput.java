package org.jboss.jws.diag.validate.output;

import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.validate.model.InstanceValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Human-readable output for {@code validate --all}: one section per instance, using the
 * single-instance layout, then an overall count.
 */
public class MultiHumanReadableOutput {

    private static final String RULE = "─".repeat(80);

    private final HumanReadableOutput single = new HumanReadableOutput();

    public void print(int instanceCount, List<InstanceValidationResult> results) {
        System.out.printf("Validated %d of %d instance(s).%n", results.size(), instanceCount);

        List<Finding> allFindings = new ArrayList<>();
        for (InstanceValidationResult result : results) {
            System.out.printf("%nInstance PID %d  %s%n", result.getPid(),
                    result.getCatalinaBase().toString().replace('\\', '/'));
            System.out.println(RULE);
            single.print(result.getFindings());
            allFindings.addAll(result.getFindings());
        }

        FindingSummary overall = new FindingSummary(allFindings);
        System.out.printf("%nOverall: %d error(s), %d warning(s), %d info(s) across %d instance(s)%n",
                overall.getErrors(), overall.getWarnings(), overall.getInfo(), results.size());
    }
}
