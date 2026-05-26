package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about native libraries (APR, OpenSSL) loaded by Tomcat.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NativeInfo {

    private final String aprVersion;
    private final String opensslVersion;
    private final boolean loaded;

    private NativeInfo(Builder builder) {
        this.aprVersion = builder.aprVersion;
        this.opensslVersion = builder.opensslVersion;
        this.loaded = builder.loaded;
    }

    @JsonProperty("aprVersion")
    public String getAprVersion() {
        return aprVersion;
    }

    @JsonProperty("opensslVersion")
    public String getOpensslVersion() {
        return opensslVersion;
    }

    @JsonProperty("loaded")
    public boolean isLoaded() {
        return loaded;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String aprVersion;
        private String opensslVersion;
        private boolean loaded;

        public Builder aprVersion(String aprVersion) {
            this.aprVersion = aprVersion;
            return this;
        }

        public Builder opensslVersion(String opensslVersion) {
            this.opensslVersion = opensslVersion;
            return this;
        }

        public Builder loaded(boolean loaded) {
            this.loaded = loaded;
            return this;
        }

        public NativeInfo build() {
            return new NativeInfo(this);
        }
    }

    @Override
    public String toString() {
        return "NativeInfo{aprVersion='" + aprVersion + "', opensslVersion='" + opensslVersion
                + "', loaded=" + loaded + '}';
    }
}
