package org.jboss.jws.diag.config.formatter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jws.diag.config.model.ConfigValue;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.EngineConfig;
import org.jboss.jws.diag.config.model.HostConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.model.ServiceConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigJsonFormatterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConfigJsonFormatter formatter = new ConfigJsonFormatter();

    private static ServerConfig minimalServer() {
        return ServerConfig.builder()
                .shutdownPort(8005)
                .shutdownCommand("SHUTDOWN")
                .listeners(Collections.emptyList())
                .services(List.of(ServiceConfig.builder()
                        .name("Catalina")
                        .connectors(List.of(ConnectorConfig.builder()
                                .port(8080)
                                .protocol(ConfigValue.explicit("HTTP/1.1"))
                                .sslEnabled(ConfigValue.defaulted(false))
                                .maxThreads(ConfigValue.defaulted(200))
                                .connectionTimeout(ConfigValue.explicit(20000))
                                .maxConnections(ConfigValue.defaulted(8192))
                                .compression(ConfigValue.defaulted("off"))
                                .build()))
                        .executors(Collections.emptyList())
                        .engine(EngineConfig.builder()
                                .name("Catalina")
                                .defaultHost("localhost")
                                .hosts(List.of(HostConfig.builder()
                                        .name("localhost")
                                        .appBase(ConfigValue.explicit("webapps"))
                                        .autoDeploy(ConfigValue.explicit(true))
                                        .unpackWARs(ConfigValue.explicit(true))
                                        .build()))
                                .build())
                        .build()))
                .build();
    }

    @Test
    void outputIsValidJson() throws Exception {
        String json = formatter.format(minimalServer());
        assertThat(MAPPER.readTree(json)).isNotNull();
    }

    @Test
    void schemaVersionPresentAtRoot() throws Exception {
        JsonNode root = MAPPER.readTree(formatter.format(minimalServer()));
        assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
    }

    @Test
    void schemaVersionIsFirstField() throws Exception {
        String json = formatter.format(minimalServer());
        assertThat(json.indexOf("schemaVersion")).isLessThan(json.indexOf("shutdownPort"));
    }

    @Test
    void shutdownPortPresent() throws Exception {
        JsonNode root = MAPPER.readTree(formatter.format(minimalServer()));
        assertThat(root.get("shutdownPort").asInt()).isEqualTo(8005);
    }

    @Test
    void configValueSerializedWithProvenance() throws Exception {
        JsonNode root = MAPPER.readTree(formatter.format(minimalServer()));
        JsonNode protocol = root.get("services").get(0)
                .get("connectors").get(0).get("protocol");
        assertThat(protocol.get("value").asText()).isEqualTo("HTTP/1.1");
        assertThat(protocol.get("explicit").asBoolean()).isTrue();

        JsonNode maxThreads = root.get("services").get(0)
                .get("connectors").get(0).get("maxThreads");
        assertThat(maxThreads.get("value").asInt()).isEqualTo(200);
        assertThat(maxThreads.get("explicit").asBoolean()).isFalse();
    }

    @Test
    void nullFieldsExcluded() throws Exception {
        JsonNode connector = MAPPER.readTree(formatter.format(minimalServer()))
                .get("services").get(0).get("connectors").get(0);
        assertThat(connector.has("executorRef")).isFalse();
        assertThat(connector.has("proxyName")).isFalse();
        assertThat(connector.has("ssl")).isFalse();
        assertThat(connector.has("secretRequired")).isFalse();
    }

    @Test
    void compressionSerializedAsString() throws Exception {
        JsonNode compression = MAPPER.readTree(formatter.format(minimalServer()))
                .get("services").get(0).get("connectors").get(0).get("compression");
        assertThat(compression.get("value").asText()).isEqualTo("off");
    }

    @Test
    void outputIsIndented() {
        String json = formatter.format(minimalServer());
        assertThat(json).contains("\n");
        assertThat(json).contains("  ");
    }
}
