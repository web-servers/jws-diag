package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * Represents a {@code <Connector>} element in {@code server.xml}.
 *
 * <p>Fields with Tomcat compiled-in defaults are wrapped in {@link ConfigValue}
 * so callers can distinguish explicitly configured values from defaults.
 * {@code port} is always required and therefore stored as a plain {@code int}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ConnectorConfig {

    private final int port;
    private final ConfigValue<String> protocol;
    private final ConfigValue<Boolean> sslEnabled;
    private final ConfigValue<Integer> maxThreads;
    private final ConfigValue<Integer> connectionTimeout;
    private final ConfigValue<Integer> maxConnections;
    private final String executorRef;
    private final String proxyName;
    private final Integer proxyPort;
    private final ConfigValue<String> compression;
    private final ConfigValue<Boolean> secretRequired;
    private final List<SslHostConfig> sslHostConfigs;

    private ConnectorConfig(Builder b) {
        this.port = b.port;
        this.protocol = b.protocol;
        this.sslEnabled = b.sslEnabled;
        this.maxThreads = b.maxThreads;
        this.connectionTimeout = b.connectionTimeout;
        this.maxConnections = b.maxConnections;
        this.executorRef = b.executorRef;
        this.proxyName = b.proxyName;
        this.proxyPort = b.proxyPort;
        this.compression = b.compression;
        this.secretRequired = b.secretRequired;
        this.sslHostConfigs = b.sslHostConfigs != null
                ? Collections.unmodifiableList(b.sslHostConfigs) : null;
    }

    public int getPort() { return port; }
    public ConfigValue<String> getProtocol() { return protocol; }
    public ConfigValue<Boolean> getSslEnabled() { return sslEnabled; }
    public ConfigValue<Integer> getMaxThreads() { return maxThreads; }
    public ConfigValue<Integer> getConnectionTimeout() { return connectionTimeout; }
    public ConfigValue<Integer> getMaxConnections() { return maxConnections; }
    public String getExecutorRef() { return executorRef; }
    public String getProxyName() { return proxyName; }
    public Integer getProxyPort() { return proxyPort; }
    public ConfigValue<String> getCompression() { return compression; }
    public ConfigValue<Boolean> getSecretRequired() { return secretRequired; }
    public List<SslHostConfig> getSslHostConfigs() { return sslHostConfigs; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int port;
        private ConfigValue<String> protocol;
        private ConfigValue<Boolean> sslEnabled;
        private ConfigValue<Integer> maxThreads;
        private ConfigValue<Integer> connectionTimeout;
        private ConfigValue<Integer> maxConnections;
        private String executorRef;
        private String proxyName;
        private Integer proxyPort;
        private ConfigValue<String> compression;
        private ConfigValue<Boolean> secretRequired;
        private List<SslHostConfig> sslHostConfigs;

        public Builder port(int v) { this.port = v; return this; }
        public Builder protocol(ConfigValue<String> v) { this.protocol = v; return this; }
        public Builder sslEnabled(ConfigValue<Boolean> v) { this.sslEnabled = v; return this; }
        public Builder maxThreads(ConfigValue<Integer> v) { this.maxThreads = v; return this; }
        public Builder connectionTimeout(ConfigValue<Integer> v) { this.connectionTimeout = v; return this; }
        public Builder maxConnections(ConfigValue<Integer> v) { this.maxConnections = v; return this; }
        public Builder executorRef(String v) { this.executorRef = v; return this; }
        public Builder proxyName(String v) { this.proxyName = v; return this; }
        public Builder proxyPort(Integer v) { this.proxyPort = v; return this; }
        public Builder compression(ConfigValue<String> v) { this.compression = v; return this; }
        public Builder secretRequired(ConfigValue<Boolean> v) { this.secretRequired = v; return this; }
        public Builder sslHostConfigs(List<SslHostConfig> v) { this.sslHostConfigs = v; return this; }

        public ConfigValue<String> getProtocol() { return protocol; }
        public ConfigValue<Boolean> getSslEnabled() { return sslEnabled; }
        public ConfigValue<Integer> getMaxThreads() { return maxThreads; }
        public ConfigValue<Integer> getConnectionTimeout() { return connectionTimeout; }
        public ConfigValue<Integer> getMaxConnections() { return maxConnections; }
        public ConfigValue<String> getCompression() { return compression; }
        public ConfigValue<Boolean> getSecretRequired() { return secretRequired; }

        public ConnectorConfig build() { return new ConnectorConfig(this); }
    }

    @Override
    public String toString() {
        return "ConnectorConfig{port=" + port + ", protocol=" + protocol
                + ", sslEnabled=" + sslEnabled + ", maxThreads=" + maxThreads + '}';
    }
}
