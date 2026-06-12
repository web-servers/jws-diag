package org.jboss.jws.diag.summary.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class TomcatVersionDetectorTest {

    @TempDir
    Path tempDir;

    private Path buildCatalinaJar(String serverNumber) throws IOException {
        Path lib = tempDir.resolve("lib");
        Files.createDirectories(lib);
        Path jar = lib.resolve("catalina.jar");
        try (OutputStream fos = Files.newOutputStream(jar);
             ZipOutputStream zip = new ZipOutputStream(fos)) {
            zip.putNextEntry(new ZipEntry("org/apache/catalina/util/ServerInfo.properties"));
            String content = "server.info=Apache Tomcat/10.1.49\nserver.number=" + serverNumber + "\nserver.built=Jan 1 2024\n";
            zip.write(content.getBytes());
            zip.closeEntry();
        }
        return tempDir;
    }

    @Test
    void extractsTomcatVersionFromCatalinaJar() throws IOException {
        Path catalinaHome = buildCatalinaJar("10.1.49.0");

        String version = new TomcatVersionDetector(catalinaHome).detect();

        assertThat(version).isEqualTo("10.1.49.0");
    }

    @Test
    void returnsNullWhenCatalinaHomeIsNull() {
        assertThat(new TomcatVersionDetector(null).detect()).isNull();
    }

    @Test
    void returnsNullWhenCatalinaJarMissing() {
        assertThat(new TomcatVersionDetector(tempDir).detect()).isNull();
    }

    @Test
    void returnsNullWhenServerInfoPropertiesMissingFromJar() throws IOException {
        Path lib = tempDir.resolve("lib");
        Files.createDirectories(lib);
        Path jar = lib.resolve("catalina.jar");
        try (OutputStream fos = Files.newOutputStream(jar);
             ZipOutputStream zip = new ZipOutputStream(fos)) {
            zip.putNextEntry(new ZipEntry("some/other/file.txt"));
            zip.write("content".getBytes());
            zip.closeEntry();
        }

        assertThat(new TomcatVersionDetector(tempDir).detect()).isNull();
    }
}
