package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a {@code <Valve>} element in {@code server.xml}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ValveConfig {

    private final String className;
    private final Map<String, String> attributes;

    private ValveConfig(Builder b) {
        this.className = b.className;
        this.attributes = b.attributes != null
                ? Collections.unmodifiableMap(b.attributes) : Collections.emptyMap();
    }

    public String getClassName() { return className; }
    public Map<String, String> getAttributes() { return attributes; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String className;
        private Map<String, String> attributes;

        public Builder className(String v) { this.className = v; return this; }
        public Builder attributes(Map<String, String> v) { this.attributes = v; return this; }

        public ValveConfig build() { return new ValveConfig(this); }
    }

    @Override
    public String toString() {
        return "ValveConfig{className='" + className + "', attributes=" + attributes + '}';
    }
}
