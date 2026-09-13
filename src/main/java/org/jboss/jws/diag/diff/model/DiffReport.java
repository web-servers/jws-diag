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
 * Full diff result between two server.xml configurations.
 */
@JsonPropertyOrder({"schemaVersion", "left", "right", "changeCount", "changes"})
public final class DiffReport {

    private final Path leftBase;
    private final Path rightBase;
    private final List<DiffEntry> entries;

    public DiffReport(Path leftBase, Path rightBase, List<DiffEntry> entries) {
        this.leftBase = leftBase;
        this.rightBase = rightBase;
        this.entries = Collections.unmodifiableList(entries);
    }

    @JsonProperty("schemaVersion")
    public String getSchemaVersion() { return SchemaVersions.DIFF; }

    @JsonProperty("left")
    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getLeft() { return leftBase; }

    @JsonProperty("right")
    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getRight() { return rightBase; }

    @JsonProperty("changeCount")
    public int getChangeCount() { return entries.size(); }

    @JsonProperty("changes")
    public List<DiffEntry> getEntries() { return entries; }

    public boolean hasDifferences() { return !entries.isEmpty(); }
}
