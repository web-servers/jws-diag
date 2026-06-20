package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * Represents a {@code <Service>} element in {@code server.xml}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ServiceConfig {

    private final String name;
    private final List<ConnectorConfig> connectors;
    private final List<ExecutorConfig> executors;
    private final EngineConfig engine;

    private ServiceConfig(Builder b) {
        this.name = b.name;
        this.connectors = b.connectors != null
                ? Collections.unmodifiableList(b.connectors) : Collections.emptyList();
        this.executors = b.executors != null
                ? Collections.unmodifiableList(b.executors) : Collections.emptyList();
        this.engine = b.engine;
    }

    public String getName() { return name; }
    public List<ConnectorConfig> getConnectors() { return connectors; }
    public List<ExecutorConfig> getExecutors() { return executors; }
    public EngineConfig getEngine() { return engine; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private List<ConnectorConfig> connectors;
        private List<ExecutorConfig> executors;
        private EngineConfig engine;

        public Builder name(String v) { this.name = v; return this; }
        public Builder connectors(List<ConnectorConfig> v) { this.connectors = v; return this; }
        public Builder executors(List<ExecutorConfig> v) { this.executors = v; return this; }
        public Builder engine(EngineConfig v) { this.engine = v; return this; }

        public ServiceConfig build() { return new ServiceConfig(this); }
    }

    @Override
    public String toString() {
        return "ServiceConfig{name='" + name + "', connectors=" + connectors.size() + '}';
    }
}
