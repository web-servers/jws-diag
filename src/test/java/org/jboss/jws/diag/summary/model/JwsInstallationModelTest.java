package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwsInstallationModelTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // --- JwsInstallation ---

    @Test
    void schemaVersionIsAlways10() {
        JwsInstallation inst = JwsInstallation.builder().build();
        JsonNode json = mapper.valueToTree(inst);
        assertThat(json.get("schemaVersion").asText()).isEqualTo("1.0");
    }

    @Test
    void nullFieldsAreExcludedFromJson() {
        JwsInstallation inst = JwsInstallation.builder()
                .tomcatVersion("10.1.49")
                .build();
        JsonNode json = mapper.valueToTree(inst);
        assertThat(json.has("catalinaHome")).isFalse();
        assertThat(json.has("catalinaBase")).isFalse();
        assertThat(json.has("jwsVersion")).isFalse();
        assertThat(json.has("pid")).isFalse();
        assertThat(json.has("uptime")).isFalse();
        assertThat(json.get("tomcatVersion").asText()).isEqualTo("10.1.49");
    }

    @Test
    void pathFieldsSerializeAsPlainStrings() {
        Path home = Paths.get("/opt/tomcat");
        Path base = Paths.get("/opt/tomcat/conf");
        JwsInstallation inst = JwsInstallation.builder()
                .catalinaHome(home)
                .catalinaBase(base)
                .build();
        JsonNode json = mapper.valueToTree(inst);
        assertThat(json.get("catalinaHome").asText()).isEqualTo("/opt/tomcat");
        assertThat(json.get("catalinaBase").asText()).isEqualTo("/opt/tomcat/conf");
    }

    @Test
    void subModelJsonKeysMatchDesignDocSchema() {
        JwsInstallation inst = JwsInstallation.builder()
                .jvmInfo(JvmInfo.builder().version("17").build())
                .osInfo(OsInfo.builder().name("RHEL").build())
                .containerInfo(ContainerInfo.builder().type(ContainerType.PODMAN).build())
                .nativeInfo(NativeInfo.builder().aprVersion("1.7.2").build())
                .build();
        JsonNode json = mapper.valueToTree(inst);
        assertThat(json.has("jvm")).isTrue();
        assertThat(json.has("os")).isTrue();
        assertThat(json.has("container")).isTrue();
        assertThat(json.has("nativeLib")).isTrue();
        assertThat(json.has("jvmInfo")).isFalse();
        assertThat(json.has("osInfo")).isFalse();
        assertThat(json.has("containerInfo")).isFalse();
        assertThat(json.has("nativeInfo")).isFalse();
    }

    @Test
    void builderPopulatesAllFields() {
        JvmInfo jvm = JvmInfo.builder().version("17.0.10").build();
        OsInfo os = OsInfo.builder().name("RHEL").version("9.3").arch("x86_64").build();
        ContainerInfo container = ContainerInfo.builder()
                .type(ContainerType.PODMAN)
                .detectedVia("/run/.containerenv")
                .build();
        NativeInfo native_ = NativeInfo.builder()
                .aprVersion("1.7.2")
                .opensslVersion("3.0.9")
                .loaded(true)
                .build();

        JwsInstallation inst = JwsInstallation.builder()
                .catalinaHome(Paths.get("/opt/tomcat"))
                .catalinaBase(Paths.get("/etc/tomcat"))
                .tomcatVersion("10.1.49")
                .jwsVersion("6.1.0")
                .jvmInfo(jvm)
                .osInfo(os)
                .containerInfo(container)
                .nativeInfo(native_)
                .pid(12345)
                .uptime("2d 4h")
                .build();

        assertThat(inst.getTomcatVersion()).isEqualTo("10.1.49");
        assertThat(inst.getJwsVersion()).isEqualTo("6.1.0");
        assertThat(inst.getPid()).isEqualTo(12345);
        assertThat(inst.getUptime()).isEqualTo("2d 4h");
        assertThat(inst.getJvmInfo()).isSameAs(jvm);
        assertThat(inst.getOsInfo()).isSameAs(os);
        assertThat(inst.getContainerInfo()).isSameAs(container);
        assertThat(inst.getNativeInfo()).isSameAs(native_);
    }

    // --- JvmInfo ---

    @Test
    void jvmInfoJavaHomeSerializesAsString() {
        Path javaHome = Paths.get("/usr/lib/jvm/java-17");
        JvmInfo jvm = JvmInfo.builder()
                .version("17.0.10")
                .vendor("Red Hat")
                .javaHome(javaHome)
                .build();
        JsonNode json = mapper.valueToTree(jvm);
        assertThat(json.get("javaHome").asText()).isEqualTo("/usr/lib/jvm/java-17");
    }

    @Test
    void jvmInfoNullFieldsExcluded() {
        JvmInfo jvm = JvmInfo.builder().version("17.0.10").build();
        JsonNode json = mapper.valueToTree(jvm);
        assertThat(json.has("vendor")).isFalse();
        assertThat(json.has("javaHome")).isFalse();
        assertThat(json.has("jvmArgs")).isFalse();
    }

    @Test
    void jvmArgsListIsImmutable() {
        List<String> args = new ArrayList<>(Arrays.asList("-Xmx512m", "-Xms256m"));
        JvmInfo jvm = JvmInfo.builder().jvmArgs(args).build();
        assertThatThrownBy(() -> jvm.getJvmArgs().add("-verbose"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // --- NativeInfo ---

    @Test
    void nativeInfoLoadedNullIsExcludedFromJson() {
        NativeInfo native_ = NativeInfo.builder()
                .aprVersion("1.7.2")
                .build();
        assertThat(native_.isLoaded()).isNull();
        JsonNode json = mapper.valueToTree(native_);
        assertThat(json.has("loaded")).isFalse();
    }

    @Test
    void nativeInfoLoadedFalseIsIncludedInJson() {
        NativeInfo native_ = NativeInfo.builder().loaded(false).build();
        JsonNode json = mapper.valueToTree(native_);
        assertThat(json.get("loaded").asBoolean()).isFalse();
    }

    // --- OsInfo ---

    @Test
    void osInfoBuilderAndSerialization() {
        OsInfo os = OsInfo.builder().name("RHEL").version("9.3").arch("x86_64").build();
        JsonNode json = mapper.valueToTree(os);
        assertThat(json.get("name").asText()).isEqualTo("RHEL");
        assertThat(json.get("version").asText()).isEqualTo("9.3");
        assertThat(json.get("arch").asText()).isEqualTo("x86_64");
    }

    // --- ContainerInfo ---

    @Test
    void containerInfoEnumSerializesAsLowercaseString() {
        ContainerInfo info = ContainerInfo.builder()
                .type(ContainerType.DOCKER)
                .detectedVia("/.dockerenv")
                .build();
        JsonNode json = mapper.valueToTree(info);
        assertThat(json.get("type").asText()).isEqualTo("docker");
        assertThat(json.get("detectedVia").asText()).isEqualTo("/.dockerenv");
    }
}
