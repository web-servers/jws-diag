package org.jboss.jws.diag.diff.model;

import org.jboss.jws.diag.common.SchemaVersions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jboss.jws.diag.common.UnixPathSerializer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Result of a multi-instance diff: all running JWS instances compared against a
 * single reference instance (the one with the lowest PID).
 */
@JsonPropertyOrder({"schemaVersion", "referencePid", "referenceBase", "instanceCount", "comparisons"})
public final class MultiDiffReport {

    private final int referencePid;
    private final Path referenceBase;
    private final int instanceCount;
    private final List<InstanceDiffResult> comparisons;

    public MultiDiffReport(int referencePid, Path referenceBase,
                           int instanceCount, List<InstanceDiffResult> comparisons) {
        this.referencePid = referencePid;
        this.referenceBase = referenceBase;
        this.instanceCount = instanceCount;
        this.comparisons = Collections.unmodifiableList(comparisons);
    }

    @JsonProperty("schemaVersion")
    public String getSchemaVersion() { return SchemaVersions.DIFF; }

    @JsonProperty("referencePid")
    public int getReferencePid() { return referencePid; }

    @JsonProperty("referenceBase")
    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getReferenceBase() { return referenceBase; }

    @JsonProperty("instanceCount")
    public int getInstanceCount() { return instanceCount; }

    @JsonProperty("comparisons")
    public List<InstanceDiffResult> getComparisons() { return comparisons; }

    public boolean hasDifferences() {
        return comparisons.stream().anyMatch(InstanceDiffResult::hasDifferences);
    }
}
