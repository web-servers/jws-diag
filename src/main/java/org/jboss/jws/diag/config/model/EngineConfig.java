package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * Represents the {@code <Engine>} element inside {@code <Service>} in {@code server.xml}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EngineConfig {

    private final String name;
    private final String defaultHost;
    private final List<HostConfig> hosts;
    private final RealmConfig realm;

    private EngineConfig(Builder b) {
        this.name = b.name;
        this.defaultHost = b.defaultHost;
        this.hosts = b.hosts != null
                ? Collections.unmodifiableList(b.hosts) : Collections.emptyList();
        this.realm = b.realm;
    }

    public String getName() { return name; }
    public String getDefaultHost() { return defaultHost; }
    public List<HostConfig> getHosts() { return hosts; }
    public RealmConfig getRealm() { return realm; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private String defaultHost;
        private List<HostConfig> hosts;
        private RealmConfig realm;

        public Builder name(String v) { this.name = v; return this; }
        public Builder defaultHost(String v) { this.defaultHost = v; return this; }
        public Builder hosts(List<HostConfig> v) { this.hosts = v; return this; }
        public Builder realm(RealmConfig v) { this.realm = v; return this; }

        public EngineConfig build() { return new EngineConfig(this); }
    }

    @Override
    public String toString() {
        return "EngineConfig{name='" + name + "', defaultHost='" + defaultHost + "'}";
    }
}
