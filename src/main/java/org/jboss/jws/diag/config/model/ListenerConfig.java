package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a {@code <Listener>} element in {@code server.xml}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ListenerConfig {

    private final String className;

    private ListenerConfig(Builder b) {
        this.className = b.className;
    }

    public String getClassName() {
        return className;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String className;

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public ListenerConfig build() {
            return new ListenerConfig(this);
        }
    }

    @Override
    public String toString() {
        return "ListenerConfig{className='" + className + "'}";
    }
}
