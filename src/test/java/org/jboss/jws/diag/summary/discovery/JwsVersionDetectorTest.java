package org.jboss.jws.diag.summary.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JwsVersionDetectorTest {

    @TempDir
    Path tempDir;

    @Test
    void extractsMajorVersionFromInstallationPath() throws IOException {
        Path jws6Home = tempDir.resolve("opt/rh/jws6/root/usr/share/tomcat");
        Files.createDirectories(jws6Home.resolve("lib"));

        assertThat(new JwsVersionDetector(jws6Home).detect()).isEqualTo("6");
    }

    @Test
    void extractsVersionFromJwsJarInLib() throws IOException {
        Path lib = tempDir.resolve("lib");
        Files.createDirectories(lib);
        Files.createFile(lib.resolve("jws-6.1.0.jar"));

        assertThat(new JwsVersionDetector(tempDir).detect()).isEqualTo("6.1.0");
    }

    @Test
    void pathVersionTakesPriorityOverJarVersion() throws IOException {
        Path jws6Home = tempDir.resolve("opt/rh/jws6/root/usr/share/tomcat");
        Path lib = jws6Home.resolve("lib");
        Files.createDirectories(lib);
        Files.createFile(lib.resolve("jws-5.0.0.jar"));

        assertThat(new JwsVersionDetector(jws6Home).detect()).isEqualTo("6");
    }

    @Test
    void returnsNullForPlainTomcatWithNoJwsMarkers() throws IOException {
        Path lib = tempDir.resolve("lib");
        Files.createDirectories(lib);
        Files.createFile(lib.resolve("catalina.jar"));

        assertThat(new JwsVersionDetector(tempDir).detect()).isNull();
    }

    @Test
    void returnsNullWhenCatalinaHomeIsNull() {
        assertThat(new JwsVersionDetector(null).detect()).isNull();
    }

    @Test
    void returnsNullWhenLibDirectoryMissing() {
        assertThat(new JwsVersionDetector(tempDir).detect()).isNull();
    }
}
