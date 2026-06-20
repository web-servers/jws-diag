package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * Root model for a parsed {@code server.xml}. Corresponds to the {@code <Server>} element.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ServerConfig {

    private final int shutdownPort;
    private final String shutdownCommand;
    private final List<ListenerConfig> listeners;
    private final List<ServiceConfig> services;

    private ServerConfig(Builder b) {
        this.shutdownPort = b.shutdownPort;
        this.shutdownCommand = b.shutdownCommand;
        this.listeners = b.listeners != null
                ? Collections.unmodifiableList(b.listeners) : Collections.emptyList();
        this.services = b.services != null
                ? Collections.unmodifiableList(b.services) : Collections.emptyList();
    }

    public int getShutdownPort() { return shutdownPort; }
    public String getShutdownCommand() { return shutdownCommand; }
    public List<ListenerConfig> getListeners() { return listeners; }
    public List<ServiceConfig> getServices() { return services; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private int shutdownPort;
        private String shutdownCommand;
        private List<ListenerConfig> listeners;
        private List<ServiceConfig> services;

        public Builder shutdownPort(int v) { this.shutdownPort = v; return this; }
        public Builder shutdownCommand(String v) { this.shutdownCommand = v; return this; }
        public Builder listeners(List<ListenerConfig> v) { this.listeners = v; return this; }
        public Builder services(List<ServiceConfig> v) { this.services = v; return this; }

        public ServerConfig build() { return new ServerConfig(this); }
    }

    @Override
    public String toString() {
        return "ServerConfig{shutdownPort=" + shutdownPort
                + ", services=" + services.size() + '}';
    }
}
