package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Information about native libraries (APR, OpenSSL) loaded by Tomcat.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NativeInfo {

    private final String aprVersion;
    private final String opensslVersion;
    private final Boolean loaded;

    private NativeInfo(Builder builder) {
        this.aprVersion = builder.aprVersion;
        this.opensslVersion = builder.opensslVersion;
        this.loaded = builder.loaded;
    }

    public String getAprVersion() {
        return aprVersion;
    }

    public String getOpensslVersion() {
        return opensslVersion;
    }

    public Boolean isLoaded() {
        return loaded;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String aprVersion;
        private String opensslVersion;
        private Boolean loaded;

        public Builder aprVersion(String aprVersion) {
            this.aprVersion = aprVersion;
            return this;
        }

        public Builder opensslVersion(String opensslVersion) {
            this.opensslVersion = opensslVersion;
            return this;
        }

        public Builder loaded(Boolean loaded) {
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
