package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.JvmInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JvmDetectorTest {

    @TempDir
    Path tempDir;

    private JvmDetector detector(Map<String, String> props, Integer pid) {
        return new JvmDetector(props::get, tempDir, pid);
    }

    @Test
    void detectsVersionVendorAndHomeFromSystemProperties() {
        JvmInfo info = detector(Map.of(
                "java.version", "17.0.10",
                "java.vendor", "Red Hat, Inc.",
                "java.home", "/usr/lib/jvm/java-17"
        ), null).detect();

        assertThat(info.getVersion()).isEqualTo("17.0.10");
        assertThat(info.getVendor()).isEqualTo("Red Hat, Inc.");
        assertThat(info.getJavaHome()).isEqualTo(Path.of("/usr/lib/jvm/java-17"));
    }

    @Test
    void jvmArgsEmptyWhenNoPidProvided() {
        JvmInfo info = detector(Map.of("java.version", "17"), null).detect();
        assertThat(info.getJvmArgs()).isEmpty();
    }

    @Test
    void extractsJvmArgsFromProcCmdline() throws IOException {
        writeCmdline("1234", "/usr/bin/java\0-Xmx512m\0-Xms256m\0-Dfoo=bar\0org.apache.catalina.startup.Bootstrap\0");

        JvmInfo info = detector(Map.of(), 1234).detect();

        assertThat(info.getJvmArgs()).containsExactly("-Xmx512m", "-Xms256m", "-Dfoo=bar");
    }

    @Test
    void redactsSensitiveDFlags() throws IOException {
        writeCmdline("42", "/usr/bin/java\0-Xmx512m\0-Djavax.net.ssl.keyStorePassword=s3cret\0-Dapp.name=tomcat\0");

        JvmInfo info = detector(Map.of(), 42).detect();

        assertThat(info.getJvmArgs()).containsExactly("-Xmx512m", "-Dapp.name=tomcat");
        assertThat(info.getJvmArgs()).noneMatch(a -> a.contains("s3cret"));
    }

    @Test
    void redactsPasswordSecretCredentialKeypass() throws IOException {
        writeCmdline("99", "/usr/bin/java\0"
                + "-Dmy.password=pw\0"
                + "-Dmy.secret=s\0"
                + "-Dmy.credential=c\0"
                + "-Dmy.keypass=k\0"
                + "-Xmx256m\0");

        JvmInfo info = detector(Map.of(), 99).detect();

        assertThat(info.getJvmArgs()).containsExactly("-Xmx256m");
    }

    @Test
    void jvmArgsEmptyWhenCmdlineFileMissing() {
        Files.exists(tempDir.resolve("55")); // no cmdline file
        JvmInfo info = detector(Map.of(), 55).detect();
        assertThat(info.getJvmArgs()).isEmpty();
    }

    @Test
    void nonXNonDArgsExcluded() throws IOException {
        writeCmdline("77", "/usr/bin/java\0-Xmx512m\0-cp\0/some/path\0-Dfoo=bar\0org.Bootstrap\0");

        JvmInfo info = detector(Map.of(), 77).detect();

        assertThat(info.getJvmArgs()).containsExactly("-Xmx512m", "-Dfoo=bar");
        assertThat(info.getJvmArgs()).noneMatch(a -> a.equals("-cp") || a.equals("/some/path"));
    }

    @Test
    void nullPropertiesProduceNullFields() {
        JvmInfo info = detector(Map.of(), null).detect();

        assertThat(info.getVersion()).isNull();
        assertThat(info.getVendor()).isNull();
        assertThat(info.getJavaHome()).isNull();
    }

    @Test
    void jvmArgsListIsImmutable() throws IOException {
        writeCmdline("11", "/usr/bin/java\0-Xmx512m\0");
        JvmInfo info = detector(Map.of(), 11).detect();

        assertThat(info.getJvmArgs()).isUnmodifiable();
    }

    private void writeCmdline(String pid, String content) throws IOException {
        Path pidDir = tempDir.resolve(pid);
        Files.createDirectories(pidDir);
        Files.write(pidDir.resolve("cmdline"), content.getBytes(StandardCharsets.UTF_8));
    }
}
