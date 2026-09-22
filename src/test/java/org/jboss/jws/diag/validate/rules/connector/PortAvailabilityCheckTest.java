package org.jboss.jws.diag.validate.rules.connector;

import org.jboss.jws.diag.common.RuleId;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.validate.RuleContext;
import org.jboss.jws.diag.validate.model.Finding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The rule reads listening sockets from a fixture /proc tree, so these tests never bind a
 * real socket and behave the same on every platform.
 */
public class PortAvailabilityCheckTest {

    private static final String TCP_HEADER =
            "  sl  local_address rem_address   st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode";
    private static final String TCP6_HEADER =
            "  sl  local_address                         remote_address                        st tx_queue rx_queue tr tm->when retrnsmt   uid  timeout inode";

    @TempDir
    Path proc;

    @TempDir
    Path catalinaBase;

    private final List<String> tcp = new ArrayList<>();
    private final List<String> tcp6 = new ArrayList<>();

    @BeforeEach
    void createProcNet() throws IOException {
        Files.createDirectories(proc.resolve("net"));
        writeProcNet();
    }

    // ── no /proc, nothing listening ──────────────────────────────────────────

    @Test
    void withoutProcNet_checkIsSkipped() throws Exception {
        Files.delete(proc.resolve("net/tcp"));

        assertThat(evaluate(connectors("<Connector port=\"8080\"/>"))).isEmpty();
    }

    @Test
    void nothingListening_passes() throws Exception {
        assertThat(evaluate(connectors("<Connector port=\"8080\"/>"))).isEmpty();
    }

    @Test
    void privilegedPortNobodyListensOn_passes() throws Exception {
        // Regression for #64: binding 443 as a normal user used to fail with
        // "Permission denied" and was reported as the port being in use.
        listenV4("0.0.0.0", 8080, "111");

        assertThat(evaluate(connectors("<Connector port=\"443\"/>"))).isEmpty();
    }

    @Test
    void listenerOnADifferentPort_passes() throws Exception {
        listenV4("0.0.0.0", 9090, "111");

        assertThat(evaluate(connectors("<Connector port=\"8080\"/>"))).isEmpty();
    }

    @Test
    void nonListeningSocketOnThePort_passes() throws Exception {
        // State 01 is ESTABLISHED, not LISTEN.
        tcp.add("   1: " + v4Hex("10.0.0.5") + ":" + portHex(8080) + " " + v4Hex("10.0.0.9") + ":D431 01 "
                + "00000000:00000000 00:00000000 00000000  1000        0 222 1 0000000000000000 20 4 30 10 -1");
        writeProcNet();

        assertThat(evaluate(connectors("<Connector port=\"8080\"/>"))).isEmpty();
    }

    // ── this installation is not running: any listener is a conflict ─────────

    @Test
    void wildcardListener_conflictsWhenTomcatIsNotRunning() throws Exception {
        listenV4("0.0.0.0", 8080, "111");

        List<Finding> findings = evaluate(connectors("<Connector port=\"8080\"/>"));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getRuleId()).isEqualTo(RuleId.CONN_006);
        assertThat(findings.get(0).getSeverity()).isEqualTo(Severity.WARN);
        assertThat(findings.get(0).getDetail())
                .isEqualTo("Port 8080 is already in use, so this Tomcat would fail to bind it on startup.");
    }

    @Test
    void ipv6WildcardListener_conflicts() throws Exception {
        listenV6Wildcard(8080, "111");

        assertThat(evaluate(connectors("<Connector port=\"8080\"/>"))).hasSize(1);
    }

    @Test
    void listenerOnSameSpecificAddress_conflicts() throws Exception {
        listenV4("127.0.0.1", 8080, "111");

        assertThat(evaluate(connectors("<Connector address=\"127.0.0.1\" port=\"8080\"/>"))).hasSize(1);
    }

    @Test
    void listenerOnOneAddress_conflictsWithConnectorOnAllAddresses() throws Exception {
        listenV4("127.0.0.1", 8080, "111");

        assertThat(evaluate(connectors("<Connector port=\"8080\"/>"))).hasSize(1);
    }

    @Test
    void listenerOnADifferentSpecificAddress_passes() throws Exception {
        listenV4("127.0.0.1", 8080, "111");

        assertThat(evaluate(connectors("<Connector address=\"10.0.0.5\" port=\"8080\"/>"))).isEmpty();
    }

    @Test
    void everyOccupiedPort_isReported() throws Exception {
        listenV4("0.0.0.0", 8080, "111");
        listenV4("0.0.0.0", 8443, "112");

        List<Finding> findings = evaluate(connectors("<Connector port=\"8080\"/><Connector port=\"8443\"/>"));

        assertThat(findings).hasSize(2).allMatch(f -> f.getRuleId() == RuleId.CONN_006);
    }

    // ── this installation is running: only a foreign listener conflicts ──────

    @Test
    void listenerOwnedByThisTomcat_passes() throws Exception {
        assumeSymlinks();
        runningTomcat(4242, "111");
        listenV4("0.0.0.0", 8080, "111");

        assertThat(evaluate(connectors("<Connector port=\"8080\"/>"))).isEmpty();
    }

    @Test
    void listenerOwnedByAnotherProcess_conflictsWhileTomcatRuns() throws Exception {
        assumeSymlinks();
        runningTomcat(4242, "111");
        listenV4("0.0.0.0", 8080, "999");

        List<Finding> findings = evaluate(connectors("<Connector port=\"8080\"/>"));

        assertThat(findings).hasSize(1);
        assertThat(findings.get(0).getDetail()).contains("other than this Tomcat (PID 4242)");
    }

    @Test
    void tomcatRunningButDescriptorsUnreadable_passes() throws Exception {
        // Another user's process: its /proc/<pid>/fd cannot be listed, so ownership is
        // unknown and the rule does not guess.
        runningTomcatWithoutFdDir(4242);
        listenV4("0.0.0.0", 8080, "111");

        assertThat(evaluate(connectors("<Connector port=\"8080\"/>"))).isEmpty();
    }

    // ── connectors the rule ignores ──────────────────────────────────────────

    @Test
    void connectorsWithoutUsablePort_areIgnored() throws Exception {
        listenV4("0.0.0.0", 8080, "111");

        assertThat(evaluate(connectors("<Connector/><Connector port=\"invalid\"/><Connector port=\"-1\"/>")))
                .isEmpty();
    }

    @Test
    void nullServerXml_passes() {
        RuleContext ctx = new RuleContext(catalinaBase, null, null, "testuser");

        assertThat(new PortAvailabilityCheckRule(proc).evaluate(ctx)).isEmpty();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<Finding> evaluate(Document serverXml) {
        return new PortAvailabilityCheckRule(proc).evaluate(new RuleContext(catalinaBase, serverXml, null, "testuser"));
    }

    private static Document connectors(String connectorXml) throws Exception {
        String xml = "<Server port=\"-1\" shutdown=\"SHUTDOWN\"><Service name=\"Catalina\">"
                + connectorXml + "</Service></Server>";
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        try (InputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return db.parse(in);
        }
    }

    private void listenV4(String address, int port, String inode) throws IOException {
        tcp.add("   " + tcp.size() + ": " + v4Hex(address) + ":" + portHex(port) + " 00000000:0000 0A "
                + "00000000:00000000 00:00000000 00000000  1000        0 " + inode + " 1 0000000000000000 100 0 0 10 0");
        writeProcNet();
    }

    private void listenV6Wildcard(int port, String inode) throws IOException {
        tcp6.add("   " + tcp6.size() + ": 00000000000000000000000000000000:" + portHex(port)
                + " 00000000000000000000000000000000:0000 0A 00000000:00000000 00:00000000 00000000  1000        0 "
                + inode + " 1 0000000000000000 100 0 0 10 0");
        writeProcNet();
    }

    private void writeProcNet() throws IOException {
        List<String> v4 = new ArrayList<>(List.of(TCP_HEADER));
        v4.addAll(tcp);
        Files.write(proc.resolve("net/tcp"), v4, StandardCharsets.US_ASCII);
        List<String> v6 = new ArrayList<>(List.of(TCP6_HEADER));
        v6.addAll(tcp6);
        Files.write(proc.resolve("net/tcp6"), v6, StandardCharsets.US_ASCII);
    }

    // /proc/net/tcp writes the address word in host byte order.
    private static String v4Hex(String dotted) {
        String[] parts = dotted.split("\\.");
        int[] bytes = new int[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = Integer.parseInt(parts[i]);
        }
        if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
            return String.format("%02X%02X%02X%02X", bytes[3], bytes[2], bytes[1], bytes[0]);
        }
        return String.format("%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]);
    }

    private static String portHex(int port) {
        return String.format("%04X", port);
    }

    private void runningTomcat(int pid, String... socketInodes) throws IOException {
        runningTomcatWithoutFdDir(pid);
        Path fdDir = Files.createDirectories(proc.resolve(pid + "/fd"));
        int fd = 3;
        for (String inode : socketInodes) {
            Files.createSymbolicLink(fdDir.resolve(Integer.toString(fd++)), Path.of("socket:[" + inode + "]"));
        }
    }

    private void runningTomcatWithoutFdDir(int pid) throws IOException {
        Path pidDir = Files.createDirectories(proc.resolve(Integer.toString(pid)));
        String cmdline = String.join("\0", "/usr/bin/java",
                "-Dcatalina.home=" + catalinaBase, "-Dcatalina.base=" + catalinaBase,
                "org.apache.catalina.startup.Bootstrap", "start") + "\0";
        Files.write(pidDir.resolve("cmdline"), cmdline.getBytes(StandardCharsets.UTF_8));
    }

    // /proc/<pid>/fd entries are links to "socket:[inode]". Some platforms cannot create
    // symbolic links at all, and Windows rejects ':' in a path, so probe with that exact
    // kind of target. The rule itself never reaches this code where there is no /proc.
    private void assumeSymlinks() {
        try {
            Path probe = proc.resolve("symlink-probe");
            Files.createSymbolicLink(probe, Path.of("socket:[1]"));
            Files.delete(probe);
        } catch (IOException | UnsupportedOperationException | java.nio.file.InvalidPathException e) {
            assumeTrue(false, "socket-style symbolic links not available on this platform");
        }
    }
}
