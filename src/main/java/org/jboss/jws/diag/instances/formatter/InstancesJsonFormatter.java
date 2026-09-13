package org.jboss.jws.diag.instances.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.jws.diag.common.SchemaVersions;
import org.jboss.jws.diag.instances.model.TomcatInstance;

import java.util.List;

/**
 * Serializes discovered Tomcat instances as indented JSON.
 *
 * <p>Schema:
 * <pre>
 * {
 *   "schemaVersion": "1.0",
 *   "count": 2,
 *   "instances": [
 *     { "pid": 12345, "catalinaHome": "/opt/...", "catalinaBase": "/opt/..." },
 *     ...
 *   ]
 * }
 * </pre>
 */
public final class InstancesJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(List<TomcatInstance> instances) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("schemaVersion", SchemaVersions.INSTANCES);
            root.put("count", instances.size());
            ArrayNode arr = root.putArray("instances");
            for (TomcatInstance inst : instances) {
                arr.add(MAPPER.valueToTree(inst));
            }
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize instances to JSON", e);
        }
    }
}
