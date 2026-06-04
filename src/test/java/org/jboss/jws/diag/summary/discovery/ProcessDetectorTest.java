package org.jboss.jws.diag.summary.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessDetectorTest {

    @TempDir
    Path tempDir;

    private static byte[] cmdline(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) {
            sb.append(arg).append('\0');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Path createProcEntry(String pid, byte[] cmdlineBytes) throws IOException {
        Path pidDir = tempDir.resolve(pid);
        Files.createDirectories(pidDir);
        Files.write(pidDir.resolve("cmdline"), cmdlineBytes);
        return pidDir;
    }

    @Test
    void detectsRunningTomcatProcess() throws IOException {
        Path catHome = tempDir.resolve("catalina-home");
        Files.createDirectories(catHome);

        createProcEntry("12345", cmdline(
            "/usr/bin/java",
            "-Dcatalina.home=" + catHome,
            "-Dcatalina.base=" + catHome,
            "org.apache.catalina.startup.Bootstrap",
            "start"
        ));

        Optional<ProcessDetector.Result> result = new ProcessDetector(tempDir).detect();

        assertThat(result).isPresent();
        assertThat(result.get().getPid()).isEqualTo(12345);
        assertThat(result.get().getCatalinaHome()).isEqualTo(catHome);
    }

    @Test
    void extractsCatalinaBaseSeparatelyFromHome() throws IOException {
        Path catHome = tempDir.resolve("home");
        Path catBase = tempDir.resolve("base");
        Files.createDirectories(catHome);
        Files.createDirectories(catBase);

        createProcEntry("99", cmdline(
            "/usr/bin/java",
            "-Dcatalina.home=" + catHome,
            "-Dcatalina.base=" + catBase,
            "org.apache.catalina.startup.Bootstrap",
            "start"
        ));

        ProcessDetector.Result result = new ProcessDetector(tempDir).detect().orElseThrow();
        assertThat(result.getCatalinaHome()).isEqualTo(catHome);
        assertThat(result.getCatalinaBase()).isEqualTo(catBase);
    }

    @Test
    void ignoresNonTomcatProcesses() throws IOException {
        createProcEntry("111", cmdline("/bin/bash", "-c", "echo hello"));

        assertThat(new ProcessDetector(tempDir).detect()).isEmpty();
    }

    @Test
    void skipsEntriesWithNoCmdlineFile() throws IOException {
        Files.createDirectories(tempDir.resolve("222"));
        // no cmdline file

        assertThat(new ProcessDetector(tempDir).detect()).isEmpty();
    }

    @Test
    void returnsEmptyWhenProcRootDoesNotExist() {
        ProcessDetector detector = new ProcessDetector(tempDir.resolve("nonexistent"));
        assertThat(detector.detect()).isEmpty();
    }

    @Test
    void skipsNonNumericDirectoryEntries() throws IOException {
        // "cmdline" as a directory name is not a PID dir — should be ignored
        Files.createDirectories(tempDir.resolve("not-a-pid"));

        assertThat(new ProcessDetector(tempDir).detect()).isEmpty();
    }

    @Test
    void catalinaHomeNullWhenDirectoryDoesNotExist() throws IOException {
        // Path in cmdline exists as a string but the directory is missing
        createProcEntry("55", cmdline(
            "/usr/bin/java",
            "-Dcatalina.home=/does/not/exist",
            "org.apache.catalina.startup.Bootstrap",
            "start"
        ));

        ProcessDetector.Result result = new ProcessDetector(tempDir).detect().orElseThrow();
        assertThat(result.getCatalinaHome()).isNull();
    }
}
