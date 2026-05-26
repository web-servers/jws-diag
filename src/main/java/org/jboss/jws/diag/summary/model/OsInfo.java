package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Operating system information collected from {@code /etc/os-release} or system properties.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class OsInfo {

    private final String name;
    private final String version;
    private final String arch;

    private OsInfo(Builder builder) {
        this.name = builder.name;
        this.version = builder.version;
        this.arch = builder.arch;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("arch")
    public String getArch() {
        return arch;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;
        private String version;
        private String arch;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder arch(String arch) {
            this.arch = arch;
            return this;
        }

        public OsInfo build() {
            return new OsInfo(this);
        }
    }

    @Override
    public String toString() {
        return "OsInfo{name='" + name + "', version='" + version + "', arch='" + arch + "'}";
    }
}
