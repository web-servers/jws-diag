package org.jboss.jws.diag.logs.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.jws.diag.common.SchemaVersions;
import org.jboss.jws.diag.logs.model.LogMatch;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;

public class LogsJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(LogScanResult result) {
        try {
            ObjectNode root = MAPPER.createObjectNode();
            root.put("schemaVersion", SchemaVersions.LOGS);
            root.put("file", result.getFile().toString());
            root.put("linesScanned", result.getLinesScanned());

            ArrayNode patterns = root.putArray("patterns");
            for (LogPattern p : LogPattern.values()) {
                int count = result.countFor(p);
                ObjectNode patternNode = patterns.addObject();
                patternNode.put("id", p.getId());
                patternNode.put("label", p.getLabel());
                patternNode.put("severity", p.getSeverity().name());
                patternNode.put("count", count);

                ArrayNode matchesNode = patternNode.putArray("matches");
                for (LogMatch m : result.matchesFor(p)) {
                    ObjectNode matchNode = matchesNode.addObject();
                    matchNode.put("line", m.getLineNumber());
                    matchNode.put("text", m.getText());
                }
            }

            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize LogScanResult to JSON", e);
        }
    }
}
