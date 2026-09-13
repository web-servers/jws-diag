package org.jboss.jws.diag.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jws.diag.config.formatter.ConfigJsonFormatter;
import org.jboss.jws.diag.config.formatter.MultiConfigJsonFormatter;
import org.jboss.jws.diag.config.model.InstanceConfigResult;
import org.jboss.jws.diag.config.model.MultiConfigReport;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.parser.PropertyResolver;
import org.jboss.jws.diag.config.parser.ServerXmlParser;
import org.jboss.jws.diag.diff.formatter.DiffJsonFormatter;
import org.jboss.jws.diag.diff.formatter.MultiDiffJsonFormatter;
import org.jboss.jws.diag.diff.model.ChangeType;
import org.jboss.jws.diag.diff.model.DiffEntry;
import org.jboss.jws.diag.diff.model.DiffReport;
import org.jboss.jws.diag.diff.model.InstanceDiffResult;
import org.jboss.jws.diag.diff.model.MultiDiffReport;
import org.jboss.jws.diag.instances.formatter.InstancesJsonFormatter;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.jboss.jws.diag.logs.formatter.LogsJsonFormatter;
import org.jboss.jws.diag.logs.formatter.MultiLogsJsonFormatter;
import org.jboss.jws.diag.logs.model.InstanceLogResult;
import org.jboss.jws.diag.logs.model.LogMatch;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;
import org.jboss.jws.diag.logs.model.MultiLogReport;
import org.jboss.jws.diag.modcluster.formatter.ModClusterJsonFormatter;
import org.jboss.jws.diag.modcluster.model.ModClusterConfig;
import org.jboss.jws.diag.summary.formatter.MultiSummaryJsonFormatter;
import org.jboss.jws.diag.summary.formatter.SummaryJsonFormatter;
import org.jboss.jws.diag.summary.model.ContainerInfo;
import org.jboss.jws.diag.summary.model.ContainerType;
import org.jboss.jws.diag.summary.model.JvmInfo;
import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.MultiSummaryReport;
import org.jboss.jws.diag.summary.model.NativeInfo;
import org.jboss.jws.diag.summary.model.OsInfo;
import org.jboss.jws.diag.validate.model.Finding;
import org.jboss.jws.diag.validate.model.InstanceValidationResult;
import org.jboss.jws.diag.validate.output.JsonOutput;
import org.jboss.jws.diag.validate.output.MultiJsonOutput;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pins the JSON shape of every command against a checked-in fingerprint, so the shape
 * cannot change without the schema version being considered.
 *
 * <p>Each fingerprint in {@code src/test/resources/schema/} lists the field paths a
 * fully populated payload produces, plus the {@link SchemaVersions} value it was
 * recorded under. When output changes, bump the command's version (major for removed,
 * renamed or retyped fields, minor for added ones), then regenerate:
 *
 * <pre>mvn test -Dtest=JsonSchemaContractTest -Djws.schema.update=true</pre>
 *
 * <p>Regeneration refuses to record a changed shape under an unchanged version.
 *
 * <p>Maps whose keys are data rather than schema, such as valve {@code attributes},
 * are recorded as a single path so fixture edits do not look like schema changes.
 *
 * <p>Coverage is limited to fields the fixtures below populate. When a model gains an
 * optional field, populate it here too, or the fingerprint will not see it.
 */
class JsonSchemaContractTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path SCHEMA_DIR = Paths.get("src", "test", "resources", "schema");
    private static final boolean UPDATE = Boolean.getBoolean("jws.schema.update");

    // Free-form maps whose keys come from server.xml attributes, not from the schema.
    // Only the map itself is part of the shape; its keys are data.
    private static final Set<String> FREE_FORM_MAPS = Set.of("attributes", "extraAttributes");

    private static final Path BASE_A = Path.of("/opt/jws-6.0/standalone");
    private static final Path BASE_B = Path.of("/opt/jws-5.7/standalone");

    @Test
    void summary() throws Exception {
        assertContract("summary", SchemaVersions.SUMMARY,
                new SummaryJsonFormatter().format(fullInstallation()));
    }

    @Test
    void summaryAll() throws Exception {
        assertContract("summary-all", SchemaVersions.SUMMARY,
                new MultiSummaryJsonFormatter().format(
                        new MultiSummaryReport(2, List.of(fullInstallation(), fullInstallation()))));
    }

    @Test
    void config() throws Exception {
        // A single server.xml cannot populate every element, so the fingerprint is the
        // union across the richest parser fixtures.
        List<String> outputs = new ArrayList<>();
        for (ServerConfig config : richConfigs()) {
            outputs.add(new ConfigJsonFormatter().format(config));
        }
        assertContract("config", SchemaVersions.CONFIG, outputs);
    }

    @Test
    void configAll() throws Exception {
        List<InstanceConfigResult> results = new ArrayList<>();
        int pid = 100;
        for (ServerConfig config : richConfigs()) {
            results.add(new InstanceConfigResult(pid++, BASE_A, config));
        }
        assertContract("config-all", SchemaVersions.CONFIG,
                new MultiConfigJsonFormatter().format(new MultiConfigReport(results.size(), results)));
    }

    @Test
    void validate() throws Exception {
        List<Finding> findings = fullFindings();
        assertContract("validate", SchemaVersions.VALIDATE, captureStdout(() -> new JsonOutput().print(findings, 1)));
    }

    @Test
    void validateAll() throws Exception {
        List<InstanceValidationResult> results = List.of(new InstanceValidationResult(100, BASE_A, fullFindings()));
        assertContract("validate-all", SchemaVersions.VALIDATE,
                captureStdout(() -> new MultiJsonOutput().print(1, results, 1)));
    }

    @Test
    void diff() throws Exception {
        assertContract("diff", SchemaVersions.DIFF, new DiffJsonFormatter().format(fullDiff()));
    }

    @Test
    void diffAll() throws Exception {
        MultiDiffReport report = new MultiDiffReport(100, BASE_A, 2,
                List.of(new InstanceDiffResult(200, BASE_B, fullDiff())));
        assertContract("diff-all", SchemaVersions.DIFF, new MultiDiffJsonFormatter().format(report));
    }

    @Test
    void logs() throws Exception {
        assertContract("logs", SchemaVersions.LOGS, new LogsJsonFormatter().format(fullLogScan()));
    }

    @Test
    void logsAll() throws Exception {
        MultiLogReport report = new MultiLogReport(1,
                List.of(new InstanceLogResult(100, BASE_A.resolve("logs/catalina.out"), fullLogScan())));
        assertContract("logs-all", SchemaVersions.LOGS, new MultiLogsJsonFormatter().format(report));
    }

    @Test
    void modcluster() throws Exception {
        ModClusterConfig config = ModClusterConfig.builder()
                .listenerClassName("org.jboss.modcluster.container.catalina.standalone.ModClusterListener")
                .connector("ajp")
                .advertise(true)
                .advertiseGroupAddress("224.0.1.105")
                .advertisePort(23364)
                .proxyList("proxy1:6666")
                .balancer("mycluster")
                .stickySession(true)
                .stickySessionCookie("JSESSIONID")
                .extraAttributes(Map.of("loadMetricClass", "org.jboss.modcluster.load.metric.impl.BusyConnectorsLoadMetric"))
                .build();
        assertContract("modcluster", SchemaVersions.MODCLUSTER,
                new ModClusterJsonFormatter().format(List.of(config)));
    }

    @Test
    void instances() throws Exception {
        assertContract("instances", SchemaVersions.INSTANCES,
                new InstancesJsonFormatter().format(List.of(new TomcatInstance(100, BASE_A, BASE_A))));
    }

    // ── fixtures ────────────────────────────────────────────────────────────

    private static JwsInstallation fullInstallation() {
        return JwsInstallation.builder()
                .catalinaHome(BASE_A)
                .catalinaBase(BASE_A)
                .tomcatVersion("10.1.49")
                .jwsVersion("6.1.0")
                .jvmInfo(JvmInfo.builder()
                        .version("17.0.10")
                        .vendor("Red Hat, Inc.")
                        .javaHome(Path.of("/usr/lib/jvm/java-17"))
                        .jvmArgs(List.of("-Xmx512m"))
                        .build())
                .osInfo(OsInfo.builder().name("RHEL").version("9.3").arch("x86_64").build())
                .containerInfo(ContainerInfo.builder()
                        .type(ContainerType.PODMAN)
                        .detectedVia("/run/.containerenv")
                        .build())
                .nativeInfo(NativeInfo.builder().aprVersion("1.7.2").opensslVersion("3.0.9").loaded(true).build())
                .pid(12345)
                .uptime("3d 4h")
                .build();
    }

    private static List<Finding> fullFindings() {
        return List.of(Finding.builder()
                .ruleId(RuleId.SEC_001)
                .category("Security")
                .severity(Severity.WARN)
                .summary("summary")
                .detail("detail")
                .file("server.xml")
                .fix("fix")
                .build());
    }

    private static DiffReport fullDiff() {
        return new DiffReport(BASE_A, BASE_B, List.of(
                new DiffEntry("server.shutdownPort", ChangeType.CHANGED, "8005", "8006"),
                new DiffEntry("connectors[8443]", ChangeType.ADDED, null, "port 8443"),
                new DiffEntry("connectors[8009]", ChangeType.REMOVED, "port 8009", null)));
    }

    private static LogScanResult fullLogScan() {
        Map<LogPattern, Integer> counts = new EnumMap<>(LogPattern.class);
        Map<LogPattern, List<LogMatch>> matches = new EnumMap<>(LogPattern.class);
        for (LogPattern pattern : LogPattern.values()) {
            counts.put(pattern, 1);
            matches.put(pattern, List.of(new LogMatch(42, "sample line")));
        }
        return new LogScanResult(BASE_A.resolve("logs/catalina.out"), 100, counts, matches);
    }

    private static List<ServerConfig> richConfigs() throws IOException, URISyntaxException {
        List<ServerConfig> configs = new ArrayList<>();
        for (String name : List.of("server-full-tls", "server-multi-connector", "server-executor",
                "server-proxy-valve", "server-multi-service", "server-vault-tls")) {
            Path path = Paths.get(JsonSchemaContractTest.class.getClassLoader()
                    .getResource("fixtures/config/" + name + ".xml").toURI());
            PropertyResolver resolver = new PropertyResolver(
                    Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
            configs.add(new ServerXmlParser(resolver).parse(path));
        }
        return configs;
    }

    // ── contract check ──────────────────────────────────────────────────────

    private static void assertContract(String name, String version, String json) throws IOException {
        assertContract(name, version, List.of(json));
    }

    private static void assertContract(String name, String version, List<String> outputs) throws IOException {
        Set<String> actual = new TreeSet<>();
        for (String json : outputs) {
            JsonNode root = MAPPER.readTree(json);
            assertThat(root.path("schemaVersion").asText())
                    .as("%s output must report SchemaVersions value", name)
                    .isEqualTo(version);
            collectPaths(root, "", actual);
        }

        Path file = SCHEMA_DIR.resolve(name + ".txt");
        Fingerprint recorded = Files.exists(file) ? Fingerprint.read(file) : null;

        if (UPDATE) {
            if (recorded != null && recorded.version.equals(version) && !recorded.paths.equals(actual)) {
                fail(name + " JSON shape changed but its schema version is still " + version
                        + ". Bump it in SchemaVersions before regenerating.\n" + describe(recorded.paths, actual));
            }
            Fingerprint.write(file, name, version, actual);
            return;
        }

        if (recorded == null) {
            fail("No schema fingerprint for " + name + " at " + file + ". Generate it with "
                    + "mvn test -Dtest=JsonSchemaContractTest -Djws.schema.update=true");
        }
        if (!recorded.version.equals(version)) {
            fail(name + " schema version is " + version + " but the fingerprint was recorded under "
                    + recorded.version + ". Regenerate with -Djws.schema.update=true.");
        }
        if (!recorded.paths.equals(actual)) {
            fail(name + " JSON shape no longer matches its fingerprint. Bump SchemaVersions ("
                    + "major for removed, renamed or retyped fields, minor for added ones), then "
                    + "regenerate with -Djws.schema.update=true.\n" + describe(recorded.paths, actual));
        }
    }

    // Arrays collapse to "[]" so the fingerprint describes shape, not how many items a fixture had.
    private static void collectPaths(JsonNode node, String prefix, Set<String> paths) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String path = prefix.isEmpty() ? field.getKey() : prefix + "." + field.getKey();
                paths.add(path);
                if (!FREE_FORM_MAPS.contains(field.getKey())) {
                    collectPaths(field.getValue(), path, paths);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                collectPaths(element, prefix + "[]", paths);
            }
        }
    }

    private static String describe(Set<String> recorded, Set<String> actual) {
        Set<String> added = new TreeSet<>(actual);
        added.removeAll(recorded);
        Set<String> removed = new TreeSet<>(recorded);
        removed.removeAll(actual);
        return "  added:   " + added + "\n  removed: " + removed;
    }

    private static String captureStdout(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    private static final class Fingerprint {
        private static final String VERSION_PREFIX = "schemaVersion=";

        final String version;
        final Set<String> paths;

        private Fingerprint(String version, Set<String> paths) {
            this.version = version;
            this.paths = paths;
        }

        static Fingerprint read(Path file) throws IOException {
            String version = null;
            Set<String> paths = new TreeSet<>();
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                if (trimmed.startsWith(VERSION_PREFIX)) {
                    version = trimmed.substring(VERSION_PREFIX.length());
                } else {
                    paths.add(trimmed);
                }
            }
            return new Fingerprint(String.valueOf(version), paths);
        }

        static void write(Path file, String name, String version, Set<String> paths) throws IOException {
            List<String> lines = new ArrayList<>();
            lines.add("# JSON field paths for " + name + ", checked by JsonSchemaContractTest.");
            lines.add("# Regenerate: mvn test -Dtest=JsonSchemaContractTest -Djws.schema.update=true");
            lines.add(VERSION_PREFIX + version);
            lines.addAll(paths);
            Files.createDirectories(file.getParent());
            Files.write(file, lines, StandardCharsets.UTF_8);
        }
    }
}
