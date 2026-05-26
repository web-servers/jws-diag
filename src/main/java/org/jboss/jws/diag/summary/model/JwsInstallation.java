package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.nio.file.Path;

/**
 * Root model representing a discovered JBoss Web Server / Apache Tomcat installation.
 * All fields are optional; discovery may populate a subset depending on what is detectable.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JwsInstallation {

    private final Path catalinaHome;
    private final Path catalinaBase;
    private final String tomcatVersion;
    private final String jwsVersion;
    private final JvmInfo jvmInfo;
    private final OsInfo osInfo;
    private final ContainerInfo containerInfo;
    private final NativeInfo nativeInfo;
    private final Integer pid;

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
    }

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonProperty("catalinaHome")
    public Path getCatalinaHome() {
        return catalinaHome;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonProperty("catalinaBase")
    public Path getCatalinaBase() {
        return catalinaBase;
    }

    @JsonProperty("tomcatVersion")
    public String getTomcatVersion() {
        return tomcatVersion;
    }

    @JsonProperty("jwsVersion")
    public String getJwsVersion() {
        return jwsVersion;
    }

    @JsonProperty("jvmInfo")
    public JvmInfo getJvmInfo() {
        return jvmInfo;
    }

    @JsonProperty("osInfo")
    public OsInfo getOsInfo() {
        return osInfo;
    }

    @JsonProperty("containerInfo")
    public ContainerInfo getContainerInfo() {
        return containerInfo;
    }

    @JsonProperty("nativeInfo")
    public NativeInfo getNativeInfo() {
        return nativeInfo;
    }

    @JsonProperty("pid")
    public Integer getPid() {
        return pid;
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
                + ", jvmInfo=" + jvmInfo
                + ", osInfo=" + osInfo
                + ", containerInfo=" + containerInfo
                + ", nativeInfo=" + nativeInfo
                + '}';
    }
}
