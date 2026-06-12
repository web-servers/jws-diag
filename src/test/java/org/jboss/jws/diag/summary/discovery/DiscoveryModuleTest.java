package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.ContainerType;
import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class DiscoveryModuleTest {

    @TempDir
    Path tempDir;

    private Path validTomcatHome(String name) throws IOException {
        Path home = tempDir.resolve(name);
        Files.createDirectories(home.resolve("lib"));
        Files.createDirectories(home.resolve("conf"));
        Files.createFile(home.resolve("lib/catalina.jar"));
        Files.createFile(home.resolve("conf/server.xml"));
        return home;
    }

    private Path tomcatHomeWithVersion(String name, String version) throws IOException {
        Path home = tempDir.resolve(name);
        Path lib = home.resolve("lib");
        Files.createDirectories(lib);
        Files.createDirectories(home.resolve("conf"));
        Files.createFile(home.resolve("conf/server.xml"));

        Path jar = lib.resolve("catalina.jar");
        try (OutputStream fos = Files.newOutputStream(jar);
             ZipOutputStream zip = new ZipOutputStream(fos)) {
            zip.putNextEntry(new ZipEntry("org/apache/catalina/util/ServerInfo.properties"));
            zip.write(("server.number=" + version + "\n").getBytes());
            zip.closeEntry();
        }
        return home;
    }

    private DiscoveryModule module(Path cliHome, Map<String, String> env,
                                   Map<String, String> sysProps, Path procRoot) {
        CatalinaDiscovery catalina = new CatalinaDiscovery(
                cliHome, null, env::get, List.of(),
                new WellKnownPaths(tempDir),
                new ProcessDetector(procRoot)
        );
        return new DiscoveryModule(
                catalina,
                new OsDetector(tempDir, sysProps::get),
                new ContainerDetector(tempDir, env::get),
                sysProps::get,
                procRoot
        );
    }

    @Test
    void populatesCatalinaHomeAndBase() throws IOException {
        Path home = validTomcatHome("home");

        JwsInstallation result = module(home, Map.of(), Map.of(), tempDir.resolve("no-proc")).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(home);
        assertThat(result.getCatalinaBase()).isEqualTo(home);
    }

    @Test
    void populatesTomcatVersionFromCatalinaJar() throws IOException {
        Path home = tomcatHomeWithVersion("home", "10.1.49.0");

        JwsInstallation result = module(home, Map.of(), Map.of(), tempDir.resolve("no-proc")).discover();

        assertThat(result.getTomcatVersion()).isEqualTo("10.1.49.0");
    }

    @Test
    void populatesJwsVersionFromPath() throws IOException {
        Path jws6Home = tempDir.resolve("opt/rh/jws6/root/usr/share/tomcat");
        Files.createDirectories(jws6Home.resolve("lib"));
        Files.createDirectories(jws6Home.resolve("conf"));
        Files.createFile(jws6Home.resolve("lib/catalina.jar"));
        Files.createFile(jws6Home.resolve("conf/server.xml"));

        JwsInstallation result = module(jws6Home, Map.of(), Map.of(), tempDir.resolve("no-proc")).discover();

        assertThat(result.getJwsVersion()).isEqualTo("6");
    }

    @Test
    void populatesJvmInfoFromSystemProperties() throws IOException {
        Path home = validTomcatHome("home");
        Map<String, String> props = Map.of(
                "java.version", "17.0.10",
                "java.vendor", "Red Hat, Inc.",
                "java.home", "/usr/lib/jvm/java-17"
        );

        JwsInstallation result = module(home, Map.of(), props, tempDir.resolve("no-proc")).discover();

        assertThat(result.getJvmInfo().getVersion()).isEqualTo("17.0.10");
        assertThat(result.getJvmInfo().getVendor()).isEqualTo("Red Hat, Inc.");
    }

    @Test
    void populatesOsInfoFromSystemPropertiesWhenNoOsRelease() throws IOException {
        Path home = validTomcatHome("home");
        Map<String, String> props = Map.of(
                "os.name", "Linux",
                "os.version", "6.1.0",
                "os.arch", "amd64"
        );

        JwsInstallation result = module(home, Map.of(), props, tempDir.resolve("no-proc")).discover();

        assertThat(result.getOsInfo().getName()).isEqualTo("Linux");
        assertThat(result.getOsInfo().getArch()).isEqualTo("amd64");
    }

    @Test
    void detectsDockerContainer() throws IOException {
        Path home = validTomcatHome("home");
        Files.createFile(tempDir.resolve(".dockerenv"));

        JwsInstallation result = module(home, Map.of(), Map.of(), tempDir.resolve("no-proc")).discover();

        assertThat(result.getContainerInfo().getType()).isEqualTo(ContainerType.DOCKER);
    }

    @Test
    void populatesPidFromRunningProcess() throws IOException {
        Path home = validTomcatHome("proc-home");
        Path procRoot = tempDir.resolve("proc");
        Path pidDir = procRoot.resolve("55");
        Files.createDirectories(pidDir);
        String cmdline = "/usr/bin/java\0-Dcatalina.home=" + home
                + "\0-Dcatalina.base=" + home
                + "\0org.apache.catalina.startup.Bootstrap\0";
        Files.write(pidDir.resolve("cmdline"), cmdline.getBytes(StandardCharsets.UTF_8));

        JwsInstallation result = module(null, Map.of(), Map.of(), procRoot).discover();

        assertThat(result.getPid()).isEqualTo(55);
        assertThat(result.getCatalinaHome()).isEqualTo(home);
    }

    @Test
    void nullCatalinaHomeWhenNothingFound() {
        JwsInstallation result = module(null, Map.of(), Map.of(), tempDir.resolve("no-proc")).discover();

        assertThat(result.getCatalinaHome()).isNull();
        assertThat(result.getTomcatVersion()).isNull();
        assertThat(result.getJwsVersion()).isNull();
    }
}
