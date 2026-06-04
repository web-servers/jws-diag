package org.jboss.jws.diag.summary.discovery;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CatalinaDiscoveryTest {

    @TempDir
    Path tempDir;

    // ---- helpers --------------------------------------------------------

    private Path validTomcatHome(String name) throws IOException {
        Path home = tempDir.resolve(name);
        Files.createDirectories(home.resolve("lib"));
        Files.createDirectories(home.resolve("conf"));
        Files.createFile(home.resolve("lib/catalina.jar"));
        Files.createFile(home.resolve("conf/server.xml"));
        return home;
    }

    private Path writeSysconfig(String name, String content) throws IOException {
        Path file = tempDir.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private Path createProcEntry(String pid, Path catHome, Path catBase) throws IOException {
        Path pidDir = tempDir.resolve("proc").resolve(pid);
        Files.createDirectories(pidDir);
        String cmdline = "/usr/bin/java\0-Dcatalina.home=" + catHome
            + "\0-Dcatalina.base=" + catBase
            + "\0org.apache.catalina.startup.Bootstrap\0start\0";
        Files.write(pidDir.resolve("cmdline"), cmdline.getBytes(StandardCharsets.UTF_8));
        return pidDir;
    }

    private CatalinaDiscovery discovery(Path cliHome, Path cliBase,
                                        Map<String, String> envVars,
                                        List<Path> sysconfigFiles) {
        return new CatalinaDiscovery(
            cliHome, cliBase,
            envVars::get,
            sysconfigFiles,
            new WellKnownPaths(tempDir),
            new ProcessDetector(tempDir.resolve("proc"))
        );
    }

    // ---- priority order: HOME -------------------------------------------

    @Test
    void cliHomeWinsOverEnvVar() throws IOException {
        Path cliHome = validTomcatHome("cli-home");
        Path envHome = validTomcatHome("env-home");

        CatalinaDiscovery.Result result = discovery(
            cliHome, null,
            Map.of("CATALINA_HOME", envHome.toString()),
            List.of()
        ).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(cliHome);
    }

    @Test
    void envVarWinsOverSystemd() throws IOException {
        Path envHome = validTomcatHome("env-home");
        Path systemdHome = validTomcatHome("systemd-home");
        Path sysconfig = writeSysconfig("tomcat", "CATALINA_HOME=" + systemdHome + "\n");

        CatalinaDiscovery.Result result = discovery(
            null, null,
            Map.of("CATALINA_HOME", envHome.toString()),
            List.of(sysconfig)
        ).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(envHome);
    }

    @Test
    void systemdWinsOverWellKnownPaths() throws IOException {
        Path systemdHome = validTomcatHome("systemd-home");
        Path sysconfig = writeSysconfig("tomcat", "CATALINA_HOME=" + systemdHome + "\n");

        // Create a well-known path that would match
        Path wellKnown = tempDir.resolve("usr/share/tomcat");
        Files.createDirectories(wellKnown.resolve("lib"));
        Files.createDirectories(wellKnown.resolve("conf"));
        Files.createFile(wellKnown.resolve("lib/catalina.jar"));
        Files.createFile(wellKnown.resolve("conf/server.xml"));

        CatalinaDiscovery.Result result = discovery(
            null, null,
            Map.of(),
            List.of(sysconfig)
        ).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(systemdHome);
    }

    @Test
    void wellKnownPathWinsOverProcessDetection() throws IOException {
        Path wellKnown = tempDir.resolve("usr/share/tomcat");
        Files.createDirectories(wellKnown.resolve("lib"));
        Files.createDirectories(wellKnown.resolve("conf"));
        Files.createFile(wellKnown.resolve("lib/catalina.jar"));
        Files.createFile(wellKnown.resolve("conf/server.xml"));

        Path procHome = validTomcatHome("proc-home");
        createProcEntry("77", procHome, procHome);

        CatalinaDiscovery.Result result = discovery(null, null, Map.of(), List.of()).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(wellKnown);
    }

    @Test
    void processDetectionUsedAsLastResort() throws IOException {
        Path procHome = validTomcatHome("proc-home");
        createProcEntry("42", procHome, procHome);

        CatalinaDiscovery.Result result = discovery(null, null, Map.of(), List.of()).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(procHome);
        assertThat(result.getPid()).isEqualTo(42);
    }

    // ---- CATALINA_BASE defaults -----------------------------------------

    @Test
    void catalinaBaseDefaultsToHomeWhenNotSet() throws IOException {
        Path home = validTomcatHome("home");

        CatalinaDiscovery.Result result = discovery(home, null, Map.of(), List.of()).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(home);
        assertThat(result.getCatalinaBase()).isEqualTo(home);
    }

    @Test
    void catalinaBaseSetSeparatelyViaCli() throws IOException {
        Path home = validTomcatHome("home");
        Path base = tempDir.resolve("base");
        Files.createDirectories(base);

        CatalinaDiscovery.Result result = discovery(home, base, Map.of(), List.of()).discover();

        assertThat(result.getCatalinaBase()).isEqualTo(base);
        assertThat(result.getCatalinaBase()).isNotEqualTo(home);
    }

    @Test
    void catalinaBaseSetViaEnvVar() throws IOException {
        Path home = validTomcatHome("home");
        Path base = tempDir.resolve("env-base");
        Files.createDirectories(base);

        CatalinaDiscovery.Result result = discovery(
            home, null,
            Map.of("CATALINA_BASE", base.toString()),
            List.of()
        ).discover();

        assertThat(result.getCatalinaBase()).isEqualTo(base);
    }

    // ---- edge cases -----------------------------------------------------

    @Test
    void returnsNullHomeWhenNothingFound() {
        CatalinaDiscovery.Result result = discovery(null, null, Map.of(), List.of()).discover();
        assertThat(result.getCatalinaHome()).isNull();
        assertThat(result.getCatalinaBase()).isNull();
    }

    @Test
    void cliHomeIgnoredWhenPathDoesNotExist() throws IOException {
        Path nonExistent = tempDir.resolve("ghost");
        Path envHome = validTomcatHome("env-home");

        CatalinaDiscovery.Result result = discovery(
            nonExistent, null,
            Map.of("CATALINA_HOME", envHome.toString()),
            List.of()
        ).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(envHome);
    }

    @Test
    void jwsVersionedPathPreferredOverStaticWellKnown() throws IOException {
        // jws6 under opt/rh
        Path jws6Home = tempDir.resolve("opt/rh/jws6/root/usr/share/tomcat");
        Files.createDirectories(jws6Home.resolve("lib"));
        Files.createDirectories(jws6Home.resolve("conf"));
        Files.createFile(jws6Home.resolve("lib/catalina.jar"));
        Files.createFile(jws6Home.resolve("conf/server.xml"));

        // static well-known also present
        Path staticHome = tempDir.resolve("usr/share/tomcat");
        Files.createDirectories(staticHome.resolve("lib"));
        Files.createDirectories(staticHome.resolve("conf"));
        Files.createFile(staticHome.resolve("lib/catalina.jar"));
        Files.createFile(staticHome.resolve("conf/server.xml"));

        CatalinaDiscovery.Result result = discovery(null, null, Map.of(), List.of()).discover();

        assertThat(result.getCatalinaHome()).isEqualTo(jws6Home);
    }

    @Test
    void newestJwsVersionPreferredWhenMultiplePresent() throws IOException {
        for (String ver : List.of("jws5", "jws6")) {
            Path p = tempDir.resolve("opt/rh/" + ver + "/root/usr/share/tomcat");
            Files.createDirectories(p.resolve("lib"));
            Files.createDirectories(p.resolve("conf"));
            Files.createFile(p.resolve("lib/catalina.jar"));
            Files.createFile(p.resolve("conf/server.xml"));
        }

        CatalinaDiscovery.Result result = discovery(null, null, Map.of(), List.of()).discover();

        assertThat(result.getCatalinaHome().toString()).contains("jws6");
    }
}
