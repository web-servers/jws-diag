package org.jboss.jws.diag.logs.model;

import org.jboss.jws.diag.common.SchemaVersions;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Aggregates {@link InstanceLogResult} entries from all running JWS instances
 * discovered via {@code /proc} scanning.
 */
@JsonPropertyOrder({"schemaVersion", "instanceCount", "instances"})
public final class MultiLogReport {

    private final int instanceCount;
    private final List<InstanceLogResult> instances;

    public MultiLogReport(int instanceCount, List<InstanceLogResult> instances) {
        this.instanceCount = instanceCount;
        this.instances = List.copyOf(instances);
    }

    public String getSchemaVersion() {
        return SchemaVersions.LOGS;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public List<InstanceLogResult> getInstances() {
        return instances;
    }
}
