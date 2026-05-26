package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * JVM information collected from system properties and the running process.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JvmInfo {

    private final String version;
    private final String vendor;
    private final String javaHome;
    private final List<String> jvmArgs;

    private JvmInfo(Builder builder) {
        this.version = builder.version;
        this.vendor = builder.vendor;
        this.javaHome = builder.javaHome;
        this.jvmArgs = builder.jvmArgs != null
                ? Collections.unmodifiableList(builder.jvmArgs)
                : null;
    }

    @JsonProperty("version")
    public String getVersion() {
        return version;
    }

    @JsonProperty("vendor")
    public String getVendor() {
        return vendor;
    }

    @JsonProperty("javaHome")
    public String getJavaHome() {
        return javaHome;
    }

    @JsonProperty("jvmArgs")
    public List<String> getJvmArgs() {
        return jvmArgs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String version;
        private String vendor;
        private String javaHome;
        private List<String> jvmArgs;

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder vendor(String vendor) {
            this.vendor = vendor;
            return this;
        }

        public Builder javaHome(String javaHome) {
            this.javaHome = javaHome;
            return this;
        }

        public Builder jvmArgs(List<String> jvmArgs) {
            this.jvmArgs = jvmArgs;
            return this;
        }

        public JvmInfo build() {
            return new JvmInfo(this);
        }
    }

    @Override
    public String toString() {
        return "JvmInfo{version='" + version + "', vendor='" + vendor
                + "', javaHome='" + javaHome + "', jvmArgs=" + jvmArgs + '}';
    }
}
