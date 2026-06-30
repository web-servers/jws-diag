package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

/**
 * Represents a {@code <Realm>} element in {@code server.xml}.
 *
 * <p>CombinedRealm and LockOutRealm can contain nested {@code <Realm>} children,
 * captured in {@code nestedRealms}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class RealmConfig {

    private final String className;
    private final List<RealmConfig> nestedRealms;

    private RealmConfig(Builder b) {
        this.className = b.className;
        this.nestedRealms = (b.nestedRealms != null && !b.nestedRealms.isEmpty())
                ? Collections.unmodifiableList(b.nestedRealms) : null;
    }

    public String getClassName() { return className; }
    public List<RealmConfig> getNestedRealms() { return nestedRealms; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String className;
        private List<RealmConfig> nestedRealms;

        public Builder className(String v) { this.className = v; return this; }
        public Builder nestedRealms(List<RealmConfig> v) { this.nestedRealms = v; return this; }

        public RealmConfig build() { return new RealmConfig(this); }
    }

    @Override
    public String toString() {
        return "RealmConfig{className='" + className + "'}";
    }
}
