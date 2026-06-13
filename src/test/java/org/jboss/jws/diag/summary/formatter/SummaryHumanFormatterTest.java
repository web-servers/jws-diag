package org.jboss.jws.diag.summary.formatter;

import org.jboss.jws.diag.summary.model.ContainerInfo;
import org.jboss.jws.diag.summary.model.ContainerType;
import org.jboss.jws.diag.summary.model.JvmInfo;
import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.NativeInfo;
import org.jboss.jws.diag.summary.model.OsInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryHumanFormatterTest {

    private final SummaryHumanFormatter formatter = new SummaryHumanFormatter();

    private JwsInstallation fullInstallation() {
        return JwsInstallation.builder()
                .catalinaHome(Path.of("/opt/rh/jws6/root/usr/share/tomcat"))
                .catalinaBase(Path.of("/opt/rh/jws6/root/etc/tomcat"))
                .tomcatVersion("10.1.49")
                .jwsVersion("6.1.0")
                .jvmInfo(JvmInfo.builder()
                        .version("17.0.10")
                        .vendor("Red Hat, Inc.")
                        .javaHome(Path.of("/usr/lib/jvm/java-17"))
                        .build())
                .osInfo(OsInfo.builder()
                        .name("RHEL")
                        .version("9.3")
                        .arch("x86_64")
                        .build())
                .containerInfo(ContainerInfo.builder()
                        .type(ContainerType.PODMAN)
                        .detectedVia("/run/.containerenv")
                        .build())
                .nativeInfo(NativeInfo.builder()
                        .aprVersion("1.7.2")
                        .opensslVersion("3.0.9")
                        .loaded(true)
                        .build())
                .pid(12345)
                .build();
    }

    @Test
    void fullInstallationRendersAllLines() {
        String output = formatter.format(fullInstallation());

        assertThat(output).contains("Tomcat 10.1.49 | JWS 6.1.0");
        assertThat(output).contains("CATALINA_HOME: /opt/rh/jws6/root/usr/share/tomcat");
        assertThat(output).contains("CATALINA_BASE: /opt/rh/jws6/root/etc/tomcat");
        assertThat(output).contains("JVM: 17.0.10 (Red Hat, Inc.)");
        assertThat(output).contains("OS: RHEL 9.3 (x86_64)");
        assertThat(output).contains("Container: Podman");
        assertThat(output).contains("Native: APR 1.7.2, OpenSSL 3.0.9 ✓");
        assertThat(output).contains("PID: 12345");
    }

    @Test
    void jwsVersionOmittedWhenNull() {
        JwsInstallation installation = JwsInstallation.builder()
                .tomcatVersion("10.1.49")
                .build();

        String output = formatter.format(installation);

        assertThat(output).startsWith("Tomcat 10.1.49\n");
        assertThat(output).doesNotContain("JWS");
    }

    @Test
    void nullTomcatVersionShowsNa() {
        JwsInstallation installation = JwsInstallation.builder().build();

        String output = formatter.format(installation);

        assertThat(output).startsWith("Tomcat N/A");
    }

    @Test
    void nullPathsShowNa() {
        JwsInstallation installation = JwsInstallation.builder().build();

        String output = formatter.format(installation);

        assertThat(output).contains("CATALINA_HOME: N/A");
        assertThat(output).contains("CATALINA_BASE: N/A");
    }

    @Test
    void nullJvmAndOsShowNa() {
        JwsInstallation installation = JwsInstallation.builder().build();

        String output = formatter.format(installation);

        assertThat(output).contains("JVM: N/A | OS: N/A");
    }

    @Test
    void bareMetalContainerShowsNone() {
        JwsInstallation installation = JwsInstallation.builder()
                .containerInfo(ContainerInfo.builder().type(ContainerType.BARE_METAL).build())
                .build();

        String output = formatter.format(installation);

        assertThat(output).contains("Container: None");
    }

    @Test
    void nullNativeInfoOmitsNativeSegment() {
        JwsInstallation installation = JwsInstallation.builder().build();

        String output = formatter.format(installation);

        assertThat(output).doesNotContain("Native:");
    }

    @Test
    void nativeWithoutLoadedFlagOmitsCheckmark() {
        JwsInstallation installation = JwsInstallation.builder()
                .nativeInfo(NativeInfo.builder()
                        .aprVersion("1.7.2")
                        .loaded(false)
                        .build())
                .build();

        String output = formatter.format(installation);

        assertThat(output).contains("Native: APR 1.7.2");
        assertThat(output).doesNotContain("✓");
    }

    @Test
    void pidLineOmittedWhenNull() {
        JwsInstallation installation = JwsInstallation.builder().build();

        String output = formatter.format(installation);

        assertThat(output).doesNotContain("PID:");
    }

    @Test
    void dockerContainerRendersCorrectly() {
        JwsInstallation installation = JwsInstallation.builder()
                .containerInfo(ContainerInfo.builder().type(ContainerType.DOCKER).build())
                .build();

        assertThat(formatter.format(installation)).contains("Container: Docker");
    }

    @Test
    void kubernetesContainerRendersCorrectly() {
        JwsInstallation installation = JwsInstallation.builder()
                .containerInfo(ContainerInfo.builder().type(ContainerType.KUBERNETES).build())
                .build();

        assertThat(formatter.format(installation)).contains("Container: Kubernetes");
    }
}
