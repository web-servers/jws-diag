package org.jboss.jws.diag.summary.formatter;

import org.jboss.jws.diag.summary.model.ContainerInfo;
import org.jboss.jws.diag.summary.model.ContainerType;
import org.jboss.jws.diag.summary.model.JvmInfo;
import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.OsInfo;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SummaryJsonFormatterTest {

    private final SummaryJsonFormatter formatter = new SummaryJsonFormatter();

    @Test
    void outputIsValidJson() {
        JwsInstallation installation = JwsInstallation.builder()
                .tomcatVersion("10.1.49")
                .build();

        String json = formatter.format(installation);

        assertThat(json).startsWith("{");
        assertThat(json).endsWith("}");
    }

    @Test
    void containsSchemaVersion() {
        String json = formatter.format(JwsInstallation.builder().build());

        assertThat(json).contains("\"schemaVersion\"");
        assertThat(json).contains("\"1.0\"");
    }

    @Test
    void nullFieldsExcluded() {
        JwsInstallation installation = JwsInstallation.builder()
                .tomcatVersion("10.1.49")
                .build();

        String json = formatter.format(installation);

        assertThat(json).doesNotContain("jwsVersion");
        assertThat(json).doesNotContain("catalinaHome");
        assertThat(json).doesNotContain("pid");
    }

    @Test
    void fullInstallationSerializesAllFields() {
        JwsInstallation installation = JwsInstallation.builder()
                .catalinaHome(Path.of("/opt/tomcat"))
                .catalinaBase(Path.of("/opt/tomcat"))
                .tomcatVersion("10.1.49")
                .jwsVersion("6.1.0")
                .jvmInfo(JvmInfo.builder()
                        .version("17.0.10")
                        .vendor("Red Hat, Inc.")
                        .build())
                .osInfo(OsInfo.builder()
                        .name("RHEL")
                        .version("9.3")
                        .arch("x86_64")
                        .build())
                .containerInfo(ContainerInfo.builder()
                        .type(ContainerType.PODMAN)
                        .build())
                .pid(12345)
                .build();

        String json = formatter.format(installation);

        assertThat(json).contains("\"tomcatVersion\" : \"10.1.49\"");
        assertThat(json).contains("\"jwsVersion\" : \"6.1.0\"");
        assertThat(json).contains("\"version\" : \"17.0.10\"");
        assertThat(json).contains("\"name\" : \"RHEL\"");
        assertThat(json).contains("\"podman\"");
        assertThat(json).contains("\"pid\" : 12345");
    }

    @Test
    void outputIsIndented() {
        JwsInstallation installation = JwsInstallation.builder()
                .tomcatVersion("10.1.49")
                .build();

        String json = formatter.format(installation);

        assertThat(json).contains("\n");
        assertThat(json).contains("  ");
    }

    @Test
    void pathsSerializedAsStrings() {
        JwsInstallation installation = JwsInstallation.builder()
                .catalinaHome(Path.of("/opt/tomcat"))
                .catalinaBase(Path.of("/opt/tomcat"))
                .build();

        String json = formatter.format(installation);

        assertThat(json).contains("\"/opt/tomcat\"");
    }
}
