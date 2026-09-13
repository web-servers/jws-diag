package org.jboss.jws.diag.config.model;

import org.jboss.jws.diag.common.SchemaVersions;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Aggregates {@link InstanceConfigResult} entries from all running JWS instances
 * discovered via {@code /proc} scanning.
 */
@JsonPropertyOrder({"schemaVersion", "instanceCount", "instances"})
public final class MultiConfigReport {

    private final int instanceCount;
    private final List<InstanceConfigResult> instances;

    public MultiConfigReport(int instanceCount, List<InstanceConfigResult> instances) {
        this.instanceCount = instanceCount;
        this.instances = List.copyOf(instances);
    }

    public String getSchemaVersion() {
        return SchemaVersions.CONFIG;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public List<InstanceConfigResult> getInstances() {
        return instances;
    }
}
