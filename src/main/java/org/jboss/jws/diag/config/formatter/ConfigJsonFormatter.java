package org.jboss.jws.diag.config.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.jws.diag.config.model.ServerConfig;

/**
 * Formats a {@link ServerConfig} as indented JSON with {@code schemaVersion}
 * prepended at the root level. {@code ConfigValue} fields serialize as
 * {@code {"value": X, "explicit": true|false}}.
 */
public class ConfigJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(ServerConfig config) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("schemaVersion", "1.0");
            MAPPER.valueToTree(config).fields()
                    .forEachRemaining(e -> root.set(e.getKey(), e.getValue()));
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize ServerConfig to JSON", e);
        }
    }
}
