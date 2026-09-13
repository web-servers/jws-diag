package org.jboss.jws.diag.summary.model;

import org.jboss.jws.diag.common.SchemaVersions;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Aggregates {@link JwsInstallation} results from all running JWS instances
 * discovered via {@code /proc} scanning.
 */
@JsonPropertyOrder({"schemaVersion", "instanceCount", "instances"})
public final class MultiSummaryReport {

    private final int instanceCount;
    private final List<JwsInstallation> instances;

    public MultiSummaryReport(int instanceCount, List<JwsInstallation> instances) {
        this.instanceCount = instanceCount;
        this.instances = List.copyOf(instances);
    }

    public String getSchemaVersion() {
        return SchemaVersions.SUMMARY;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public List<JwsInstallation> getInstances() {
        return instances;
    }
}
