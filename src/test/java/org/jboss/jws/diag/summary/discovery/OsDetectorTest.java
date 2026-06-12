package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.OsInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OsDetectorTest {

    @TempDir
    Path tempDir;

    private Path fixture(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/fixtures/os-release/" + name).toURI());
    }

    private OsDetector detectorWithFixture(Path osRelease) throws IOException {
        Path etcDir = tempDir.resolve("etc");
        Files.createDirectories(etcDir);
        Files.copy(osRelease, etcDir.resolve("os-release"));
        return new OsDetector(tempDir, k -> null);
    }

    @Test
    void parsesRhelOsRelease() throws Exception {
        OsInfo info = detectorWithFixture(fixture("rhel")).detect();

        assertThat(info.getName()).isEqualTo("Red Hat Enterprise Linux");
        assertThat(info.getVersion()).isEqualTo("9.3");
    }

    @Test
    void parsesUbuntuOsRelease() throws Exception {
        OsInfo info = detectorWithFixture(fixture("ubuntu")).detect();

        assertThat(info.getName()).isEqualTo("Ubuntu");
        assertThat(info.getVersion()).isEqualTo("22.04");
    }

    @Test
    void parsesCentosOsRelease() throws Exception {
        OsInfo info = detectorWithFixture(fixture("centos")).detect();

        assertThat(info.getName()).isEqualTo("CentOS Stream");
        assertThat(info.getVersion()).isEqualTo("9");
    }

    @Test
    void archComesFromSystemPropertyWhenOsReleasePresent() throws Exception {
        Path etcDir = tempDir.resolve("etc");
        Files.createDirectories(etcDir);
        Files.copy(fixture("rhel"), etcDir.resolve("os-release"));
        OsDetector detector = new OsDetector(tempDir, Map.of("os.arch", "x86_64")::get);

        OsInfo info = detector.detect();

        assertThat(info.getArch()).isEqualTo("x86_64");
    }

    @Test
    void fallsBackToSystemPropertiesWhenOsReleaseMissing() {
        OsDetector detector = new OsDetector(tempDir, Map.of(
                "os.name", "Linux",
                "os.version", "6.1.0",
                "os.arch", "amd64"
        )::get);

        OsInfo info = detector.detect();

        assertThat(info.getName()).isEqualTo("Linux");
        assertThat(info.getVersion()).isEqualTo("6.1.0");
        assertThat(info.getArch()).isEqualTo("amd64");
    }

    @Test
    void returnsNullFieldsWhenOsReleaseMissingAndNoProperties() {
        OsDetector detector = new OsDetector(tempDir, k -> null);

        OsInfo info = detector.detect();

        assertThat(info.getName()).isNull();
        assertThat(info.getVersion()).isNull();
        assertThat(info.getArch()).isNull();
    }
}
