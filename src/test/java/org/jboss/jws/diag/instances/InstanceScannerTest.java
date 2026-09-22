package org.jboss.jws.diag.instances;

import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InstanceScannerTest {

    @TempDir
    Path proc;

    private static byte[] cmdline(String... args) {
        StringBuilder sb = new StringBuilder();
        for (String arg : args) sb.append(arg).append('\0');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void createProcess(String pid, byte[] cmdlineBytes) throws IOException {
        Path pidDir = proc.resolve(pid);
        Files.createDirectories(pidDir);
        Files.write(pidDir.resolve("cmdline"), cmdlineBytes);
    }

    private Path dir(String name) throws IOException {
        Path p = proc.resolve(name);
        Files.createDirectories(p);
        return p;
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test
    void emptyProc_returnsEmptyList() {
        assertThat(new InstanceScanner(proc).scan()).isEmpty();
    }

    @Test
    void nonExistentProc_returnsEmptyList() {
        assertThat(new InstanceScanner(Path.of("/nonexistent/proc")).scan()).isEmpty();
    }

    @Test
    void singleInstance_detected() throws IOException {
        Path home = dir("tomcat-home");
        Path base = dir("tomcat-base");

        createProcess("1001", cmdline(
                "/usr/bin/java",
                "-Dcatalina.home=" + home,
                "-Dcatalina.base=" + base,
                "org.apache.catalina.startup.Bootstrap", "start"
        ));

        List<TomcatInstance> instances = new InstanceScanner(proc).scan();
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getPid()).isEqualTo(1001);
        assertThat(instances.get(0).getCatalinaHome()).isEqualTo(home);
        assertThat(instances.get(0).getCatalinaBase()).isEqualTo(base);
    }

    @Test
    void multipleInstances_allDetected() throws IOException {
        Path home1 = dir("home1");
        Path home2 = dir("home2");

        createProcess("2001", cmdline(
                "/usr/bin/java", "-Dcatalina.home=" + home1,
                "org.apache.catalina.startup.Bootstrap", "start"
        ));
        createProcess("2002", cmdline(
                "/usr/bin/java", "-Dcatalina.home=" + home2,
                "org.apache.catalina.startup.Bootstrap", "start"
        ));

        List<TomcatInstance> instances = new InstanceScanner(proc).scan();
        assertThat(instances).hasSize(2);
    }

    @Test
    void nonTomcatProcesses_ignored() throws IOException {
        createProcess("9999", cmdline("/bin/bash", "-c", "sleep 9999"));

        assertThat(new InstanceScanner(proc).scan()).isEmpty();
    }

    @Test
    void resultsSortedByPidAscending() throws IOException {
        Path home = dir("h");
        byte[] cmd = cmdline("/usr/bin/java", "-Dcatalina.home=" + home,
                "org.apache.catalina.startup.Bootstrap", "start");

        createProcess("5000", cmd);
        createProcess("1000", cmd);
        createProcess("3000", cmd);

        List<TomcatInstance> instances = new InstanceScanner(proc).scan();
        assertThat(instances).extracting(TomcatInstance::getPid)
                .containsExactly(1000, 3000, 5000);
    }

    @Test
    void catalinaBaseDefaultsToHomeWhenAbsent() throws IOException {
        Path home = dir("home-only");
        createProcess("7777", cmdline(
                "/usr/bin/java",
                "-Dcatalina.home=" + home,
                "org.apache.catalina.startup.Bootstrap", "start"
        ));

        TomcatInstance inst = new InstanceScanner(proc).scan().get(0);
        assertThat(inst.getCatalinaBase()).isEqualTo(home);
    }

    @Test
    void mixedProcesses_onlyTomcatReturned() throws IOException {
        Path home = dir("t-home");
        createProcess("100", cmdline("/bin/bash", "-c", "sleep 1"));
        createProcess("200", cmdline(
                "/usr/bin/java", "-Dcatalina.home=" + home,
                "org.apache.catalina.startup.Bootstrap", "start"
        ));
        createProcess("300", cmdline("/usr/bin/python3", "server.py"));

        List<TomcatInstance> instances = new InstanceScanner(proc).scan();
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getPid()).isEqualTo(200);
    }

    @Test
    void unreadableCmdlineEntry_silentlySkipped() throws IOException {
        Path home = dir("t-home2");
        createProcess("400", cmdline(
                "/usr/bin/java", "-Dcatalina.home=" + home,
                "org.apache.catalina.startup.Bootstrap", "start"
        ));
        // Pid directory with no cmdline file — Files.isReadable returns false, entry skipped
        Files.createDirectories(proc.resolve("999"));

        List<TomcatInstance> instances = new InstanceScanner(proc).scan();
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getPid()).isEqualTo(400);
    }

    @Test
    void catalinaOut_readFromProcessEnvironment() throws IOException {
        createTomcatProcess("1001");
        writeEnviron("1001", "PATH=/usr/bin", "CATALINA_OUT=/dev/stdout", "LANG=C");

        TomcatInstance instance = new InstanceScanner(proc).scan().get(0);

        assertThat(instance.getCatalinaOut()).isEqualTo("/dev/stdout");
    }

    @Test
    void catalinaOut_nullWhenNotExported() throws IOException {
        createTomcatProcess("1001");
        writeEnviron("1001", "PATH=/usr/bin", "LANG=C");

        assertThat(new InstanceScanner(proc).scan().get(0).getCatalinaOut()).isNull();
    }

    @Test
    void catalinaOut_nullWhenEnvironUnavailable_instanceStillDetected() throws IOException {
        createTomcatProcess("1001");

        List<TomcatInstance> instances = new InstanceScanner(proc).scan();

        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getCatalinaOut()).isNull();
    }

    private void createTomcatProcess(String pid) throws IOException {
        Path home = dir("tomcat-home-" + pid);
        createProcess(pid, cmdline(
                "/usr/bin/java",
                "-Dcatalina.home=" + home,
                "org.apache.catalina.startup.Bootstrap", "start"
        ));
    }

    // environ uses the same NUL-separated layout as cmdline.
    private void writeEnviron(String pid, String... entries) throws IOException {
        Files.write(proc.resolve(pid).resolve("environ"), cmdline(entries));
    }

    @Test
    void findByCatalinaBase_returnsTheMatchingInstance() throws IOException {
        Path baseA = dir("base-a");
        Path baseB = dir("base-b");
        createProcess("1001", cmdline("/usr/bin/java", "-Dcatalina.base=" + baseA,
                "org.apache.catalina.startup.Bootstrap", "start"));
        createProcess("1002", cmdline("/usr/bin/java", "-Dcatalina.base=" + baseB,
                "org.apache.catalina.startup.Bootstrap", "start"));

        assertThat(new InstanceScanner(proc).findByCatalinaBase(baseB).getPid()).isEqualTo(1002);
    }

    @Test
    void findByCatalinaBase_returnsNullWhenNotRunning() throws IOException {
        assertThat(new InstanceScanner(proc).findByCatalinaBase(dir("idle"))).isNull();
    }
}
