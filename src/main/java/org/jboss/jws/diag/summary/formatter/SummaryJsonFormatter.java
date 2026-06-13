package org.jboss.jws.diag.summary.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jboss.jws.diag.summary.model.JwsInstallation;

/**
 * Formats a {@link JwsInstallation} as indented JSON with {@code schemaVersion}
 * at the top level. Null fields are excluded via {@code @JsonInclude(NON_NULL)}
 * on the model classes.
 */
public class SummaryJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(JwsInstallation installation) {
        try {
            return MAPPER.writeValueAsString(installation);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JwsInstallation to JSON", e);
        }
    }
}
