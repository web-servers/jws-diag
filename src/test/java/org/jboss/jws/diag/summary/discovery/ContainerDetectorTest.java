package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.ContainerInfo;
import org.jboss.jws.diag.summary.model.ContainerType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerDetectorTest {

    @TempDir
    Path tempDir;

    private ContainerDetector detector(Map<String, String> env) {
        return new ContainerDetector(tempDir, env::get);
    }

    @Test
    void detectsDockerViaDockerenvFile() throws IOException {
        Files.createFile(tempDir.resolve(".dockerenv"));

        ContainerInfo info = detector(Map.of()).detect();

        assertThat(info.getType()).isEqualTo(ContainerType.DOCKER);
        assertThat(info.getDetectedVia()).isEqualTo("/.dockerenv");
    }

    @Test
    void detectsPodmanViaContainerenvFile() throws IOException {
        Path runDir = tempDir.resolve("run");
        Files.createDirectories(runDir);
        Files.createFile(runDir.resolve(".containerenv"));

        ContainerInfo info = detector(Map.of()).detect();

        assertThat(info.getType()).isEqualTo(ContainerType.PODMAN);
        assertThat(info.getDetectedVia()).isEqualTo("/run/.containerenv");
    }

    @Test
    void detectsKubernetesViaEnvVar() {
        ContainerInfo info = detector(Map.of("KUBERNETES_SERVICE_HOST", "10.96.0.1")).detect();

        assertThat(info.getType()).isEqualTo(ContainerType.KUBERNETES);
        assertThat(info.getDetectedVia()).isEqualTo("KUBERNETES_SERVICE_HOST");
    }

    @Test
    void detectsDockerViaCgroupFile() throws IOException {
        Path procOne = tempDir.resolve("proc/1");
        Files.createDirectories(procOne);
        Files.writeString(procOne.resolve("cgroup"),
                "12:devices:/docker/abc123\n11:memory:/docker/abc123\n");

        ContainerInfo info = detector(Map.of()).detect();

        assertThat(info.getType()).isEqualTo(ContainerType.DOCKER);
        assertThat(info.getDetectedVia()).isEqualTo("/proc/1/cgroup");
    }

    @Test
    void detectsContainerdViaCgroupFile() throws IOException {
        Path procOne = tempDir.resolve("proc/1");
        Files.createDirectories(procOne);
        Files.writeString(procOne.resolve("cgroup"),
                "0::/system.slice/containerd.service\n");

        ContainerInfo info = detector(Map.of()).detect();

        assertThat(info.getType()).isEqualTo(ContainerType.CONTAINERD);
        assertThat(info.getDetectedVia()).isEqualTo("/proc/1/cgroup");
    }

    @Test
    void returnsBaremetalWhenNothingDetected() {
        ContainerInfo info = detector(Map.of()).detect();

        assertThat(info.getType()).isEqualTo(ContainerType.BARE_METAL);
        assertThat(info.getDetectedVia()).isNull();
    }

    @Test
    void dockerenvTakesPriorityOverContainerenv() throws IOException {
        Files.createFile(tempDir.resolve(".dockerenv"));
        Path runDir = tempDir.resolve("run");
        Files.createDirectories(runDir);
        Files.createFile(runDir.resolve(".containerenv"));

        ContainerInfo info = detector(Map.of()).detect();

        assertThat(info.getType()).isEqualTo(ContainerType.DOCKER);
    }

    @Test
    void containerenvTakesPriorityOverKubernetesEnvVar() throws IOException {
        Path runDir = tempDir.resolve("run");
        Files.createDirectories(runDir);
        Files.createFile(runDir.resolve(".containerenv"));

        ContainerInfo info = detector(Map.of("KUBERNETES_SERVICE_HOST", "10.0.0.1")).detect();

        assertThat(info.getType()).isEqualTo(ContainerType.PODMAN);
    }
}
