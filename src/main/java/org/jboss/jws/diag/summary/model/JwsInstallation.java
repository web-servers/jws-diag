package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jboss.jws.diag.common.UnixPathSerializer;

import java.nio.file.Path;

/**
 * Root model representing a discovered JBoss Web Server / Apache Tomcat installation.
 * All fields except {@code schemaVersion} are optional; discovery may populate a subset
 * depending on what is detectable in the environment.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JwsInstallation {

    private static final String SCHEMA_VERSION = "1.0";

    private final Path catalinaHome;
    private final Path catalinaBase;
    private final String tomcatVersion;
    private final String jwsVersion;
    private final JvmInfo jvmInfo;
    private final OsInfo osInfo;
    private final ContainerInfo containerInfo;
    private final NativeInfo nativeInfo;
    private final Integer pid;
    private final String uptime;

    private JwsInstallation(Builder builder) {
        this.catalinaHome = builder.catalinaHome;
        this.catalinaBase = builder.catalinaBase;
        this.tomcatVersion = builder.tomcatVersion;
        this.jwsVersion = builder.jwsVersion;
        this.jvmInfo = builder.jvmInfo;
        this.osInfo = builder.osInfo;
        this.containerInfo = builder.containerInfo;
        this.nativeInfo = builder.nativeInfo;
        this.pid = builder.pid;
        this.uptime = builder.uptime;
    }

    public String getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getCatalinaHome() {
        return catalinaHome;
    }

    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getCatalinaBase() {
        return catalinaBase;
    }

    public String getTomcatVersion() {
        return tomcatVersion;
    }

    public String getJwsVersion() {
        return jwsVersion;
    }

    @JsonProperty("jvm")
    public JvmInfo getJvmInfo() {
        return jvmInfo;
    }

    @JsonProperty("os")
    public OsInfo getOsInfo() {
        return osInfo;
    }

    @JsonProperty("container")
    public ContainerInfo getContainerInfo() {
        return containerInfo;
    }

    @JsonProperty("nativeLib")
    public NativeInfo getNativeInfo() {
        return nativeInfo;
    }

    public Integer getPid() {
        return pid;
    }

    public String getUptime() {
        return uptime;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Path catalinaHome;
        private Path catalinaBase;
        private String tomcatVersion;
        private String jwsVersion;
        private JvmInfo jvmInfo;
        private OsInfo osInfo;
        private ContainerInfo containerInfo;
        private NativeInfo nativeInfo;
        private Integer pid;
        private String uptime;

        public Builder catalinaHome(Path catalinaHome) {
            this.catalinaHome = catalinaHome;
            return this;
        }

        public Builder catalinaBase(Path catalinaBase) {
            this.catalinaBase = catalinaBase;
            return this;
        }

        public Builder tomcatVersion(String tomcatVersion) {
            this.tomcatVersion = tomcatVersion;
            return this;
        }

        public Builder jwsVersion(String jwsVersion) {
            this.jwsVersion = jwsVersion;
            return this;
        }

        public Builder jvmInfo(JvmInfo jvmInfo) {
            this.jvmInfo = jvmInfo;
            return this;
        }

        public Builder osInfo(OsInfo osInfo) {
            this.osInfo = osInfo;
            return this;
        }

        public Builder containerInfo(ContainerInfo containerInfo) {
            this.containerInfo = containerInfo;
            return this;
        }

        public Builder nativeInfo(NativeInfo nativeInfo) {
            this.nativeInfo = nativeInfo;
            return this;
        }

        public Builder pid(Integer pid) {
            this.pid = pid;
            return this;
        }

        public Builder uptime(String uptime) {
            this.uptime = uptime;
            return this;
        }

        public JwsInstallation build() {
            return new JwsInstallation(this);
        }
    }

    @Override
    public String toString() {
        return "JwsInstallation{"
                + "catalinaHome=" + catalinaHome
                + ", catalinaBase=" + catalinaBase
                + ", tomcatVersion='" + tomcatVersion + '\''
                + ", jwsVersion='" + jwsVersion + '\''
                + ", pid=" + pid
                + ", uptime='" + uptime + '\''
                + ", jvmInfo=" + jvmInfo
                + ", osInfo=" + osInfo
                + ", containerInfo=" + containerInfo
                + ", nativeInfo=" + nativeInfo
                + '}';
    }
}
