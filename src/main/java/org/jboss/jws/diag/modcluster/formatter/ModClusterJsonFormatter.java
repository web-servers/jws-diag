package org.jboss.jws.diag.modcluster.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.jws.diag.common.SchemaVersions;
import org.jboss.jws.diag.modcluster.model.ModClusterConfig;

import java.util.List;

/**
 * Serializes mod_cluster configuration as indented JSON.
 *
 * <p>Schema:
 * <pre>
 * {
 *   "schemaVersion": "1.0",
 *   "count": 1,
 *   "listeners": [ { "listenerClassName": "...", "connector": "ajp", ... } ]
 * }
 * </pre>
 */
public final class ModClusterJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(List<ModClusterConfig> configs) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("schemaVersion", SchemaVersions.MODCLUSTER);
            root.put("count", configs.size());
            ArrayNode arr = root.putArray("listeners");
            for (ModClusterConfig cfg : configs) {
                arr.add(MAPPER.valueToTree(cfg));
            }
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ModClusterConfig to JSON", e);
        }
    }
}
