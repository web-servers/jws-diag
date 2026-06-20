package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Wraps a configuration attribute value with a flag indicating whether it was
 * explicitly set in {@code server.xml} ({@code true}) or filled in from
 * Tomcat's compiled-in defaults ({@code false}).
 *
 * <p>Serializes as {@code {"value": <T>, "explicit": true|false}}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ConfigValue<T> {

    private final T value;
    private final boolean explicit;

    private ConfigValue(T value, boolean explicit) {
        this.value = value;
        this.explicit = explicit;
    }

    public static <T> ConfigValue<T> explicit(T value) {
        return new ConfigValue<>(value, true);
    }

    public static <T> ConfigValue<T> defaulted(T value) {
        return new ConfigValue<>(value, false);
    }

    public T getValue() {
        return value;
    }

    public boolean isExplicit() {
        return explicit;
    }

    @Override
    public String toString() {
        return value + " (" + (explicit ? "explicit" : "default") + ")";
    }
}
