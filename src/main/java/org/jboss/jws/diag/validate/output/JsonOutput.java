package org.jboss.jws.diag.validate.output;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jboss.jws.diag.validate.model.Finding;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonOutput {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final DefaultPrettyPrinter PRINTER;

    static {
        PRINTER = new DefaultPrettyPrinter();
        PRINTER.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
    }

    public void print(List<Finding> findings, int exitCode) {
        FindingSummary summary = new FindingSummary(findings);

        Map<String, Object> summaryMap = new LinkedHashMap<>();
        summaryMap.put("errors", summary.getErrors());
        summaryMap.put("warnings", summary.getWarnings());
        summaryMap.put("info", summary.getInfo());

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("findings", findings);
        output.put("summary", summaryMap);
        output.put("exitCode", exitCode);

        try {
            System.out.println(MAPPER.writer(PRINTER).writeValueAsString(output));
        } catch (Exception e) {
            System.err.println("Failed to serialize findings to JSON: " + e.getMessage());
        }
    }
}
