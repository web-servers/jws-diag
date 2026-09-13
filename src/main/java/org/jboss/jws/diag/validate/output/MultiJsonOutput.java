package org.jboss.jws.diag.validate.output;

import org.jboss.jws.diag.common.SchemaVersions;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.validate.model.InstanceValidationResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON output for {@code validate --all}. Each instance carries the same
 * {@code findings} and {@code summary} shape as single-instance {@code validate}.
 */
public class MultiJsonOutput {

    public void print(int instanceCount, List<InstanceValidationResult> results, int exitCode) {
        List<Map<String, Object>> instances = new ArrayList<>();
        List<Finding> allFindings = new ArrayList<>();

        for (InstanceValidationResult result : results) {
            Map<String, Object> instance = new LinkedHashMap<>();
            instance.put("pid", result.getPid());
            instance.put("catalinaBase", result.getCatalinaBase().toString().replace('\\', '/'));
            instance.put("findings", result.getFindings());
            instance.put("summary", JsonOutput.summaryMap(result.getFindings()));
            instances.add(instance);
            allFindings.addAll(result.getFindings());
        }

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("schemaVersion", SchemaVersions.VALIDATE);
        output.put("instanceCount", instanceCount);
        output.put("instances", instances);
        output.put("summary", JsonOutput.summaryMap(allFindings));
        output.put("exitCode", exitCode);

        JsonOutput.write(output);
    }
}
