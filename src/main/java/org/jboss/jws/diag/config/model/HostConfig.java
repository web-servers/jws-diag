package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * Represents a {@code <Host>} element inside {@code <Engine>} in {@code server.xml}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class HostConfig {

    private final String name;
    private final ConfigValue<String> appBase;
    private final ConfigValue<Boolean> autoDeploy;
    private final ConfigValue<Boolean> unpackWARs;
    private final List<ValveConfig> valves;
    private final RealmConfig realm;

    private HostConfig(Builder b) {
        this.name = b.name;
        this.appBase = b.appBase;
        this.autoDeploy = b.autoDeploy;
        this.unpackWARs = b.unpackWARs;
        this.valves = b.valves != null
                ? Collections.unmodifiableList(b.valves) : Collections.emptyList();
        this.realm = b.realm;
    }

    public String getName() { return name; }
    public ConfigValue<String> getAppBase() { return appBase; }
    public ConfigValue<Boolean> getAutoDeploy() { return autoDeploy; }
    public ConfigValue<Boolean> getUnpackWARs() { return unpackWARs; }
    public List<ValveConfig> getValves() { return valves; }
    public RealmConfig getRealm() { return realm; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private ConfigValue<String> appBase;
        private ConfigValue<Boolean> autoDeploy;
        private ConfigValue<Boolean> unpackWARs;
        private List<ValveConfig> valves;
        private RealmConfig realm;

        public Builder name(String v) { this.name = v; return this; }
        public Builder appBase(ConfigValue<String> v) { this.appBase = v; return this; }
        public Builder autoDeploy(ConfigValue<Boolean> v) { this.autoDeploy = v; return this; }
        public Builder unpackWARs(ConfigValue<Boolean> v) { this.unpackWARs = v; return this; }
        public Builder valves(List<ValveConfig> v) { this.valves = v; return this; }
        public Builder realm(RealmConfig v) { this.realm = v; return this; }

        public ConfigValue<String> getAppBase() { return appBase; }
        public ConfigValue<Boolean> getAutoDeploy() { return autoDeploy; }
        public ConfigValue<Boolean> getUnpackWARs() { return unpackWARs; }

        public HostConfig build() { return new HostConfig(this); }
    }

    @Override
    public String toString() {
        return "HostConfig{name='" + name + "', appBase=" + appBase
                + ", autoDeploy=" + autoDeploy + '}';
    }
}
