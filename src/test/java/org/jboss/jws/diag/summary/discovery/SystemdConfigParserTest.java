package org.jboss.jws.diag.summary.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemdConfigParserTest {

    @TempDir
    Path tempDir;

    private Path fixture(String name) throws URISyntaxException {
        return Path.of(getClass().getResource("/fixtures/sysconfig/" + name).toURI());
    }

    @Test
    void parsesStandardKeyValuePairs() throws URISyntaxException {
        Map<String, String> props = new SystemdConfigParser(fixture("tomcat-standard")).parse();
        assertThat(props).containsEntry("CATALINA_HOME", "/opt/tomcat")
                         .containsEntry("CATALINA_BASE", "/etc/tomcat")
                         .containsEntry("JAVA_HOME", "/usr/lib/jvm/java-11");
    }

    @Test
    void stripsExportPrefix() throws URISyntaxException {
        Map<String, String> props = new SystemdConfigParser(fixture("tomcat-export")).parse();
        assertThat(props).containsEntry("CATALINA_HOME", "/opt/rh/jws5/root/usr/share/tomcat")
                         .containsEntry("CATALINA_BASE", "/opt/rh/jws5/root/etc/tomcat");
    }

    @Test
    void stripsDoubleAndSingleQuotes() throws URISyntaxException {
        Map<String, String> props = new SystemdConfigParser(fixture("tomcat-quoted")).parse();
        assertThat(props.get("CATALINA_HOME")).isEqualTo("/opt/tomcat");
        assertThat(props.get("CATALINA_BASE")).isEqualTo("/etc/tomcat");
    }

    @Test
    void ignoresCommentedLines() throws URISyntaxException {
        Map<String, String> props = new SystemdConfigParser(fixture("tomcat-commented")).parse();
        assertThat(props.get("CATALINA_HOME")).isEqualTo("/opt/tomcat");
        assertThat(props.get("CATALINA_BASE")).isEqualTo("/etc/tomcat");
        assertThat(props).doesNotContainKey("/should/not/appear");
    }

    @Test
    void returnsEmptyMapForEmptyFile() throws URISyntaxException {
        Map<String, String> props = new SystemdConfigParser(fixture("tomcat-empty")).parse();
        assertThat(props).isEmpty();
    }

    @Test
    void returnsEmptyMapForMissingFile() {
        Map<String, String> props = new SystemdConfigParser(
            tempDir.resolve("nonexistent-file")).parse();
        assertThat(props).isEmpty();
    }

    @Test
    void ignoresLinesWithoutEqualsSign() throws IOException {
        Path file = tempDir.resolve("no-equals");
        Files.writeString(file, "INVALID_LINE\nKEY=value\n");
        Map<String, String> props = new SystemdConfigParser(file).parse();
        assertThat(props).containsOnlyKeys("KEY");
        assertThat(props.get("KEY")).isEqualTo("value");
    }

    @Test
    void ignoresBlankLines() throws IOException {
        Path file = tempDir.resolve("blank-lines");
        Files.writeString(file, "\n\nCAT_HOME=/opt/tomcat\n\n");
        Map<String, String> props = new SystemdConfigParser(file).parse();
        assertThat(props).containsEntry("CAT_HOME", "/opt/tomcat");
    }
}
