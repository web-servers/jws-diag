package org.jboss.jws.diag.summary.discovery;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves {@code CATALINA_HOME} and {@code CATALINA_BASE} through a
 * priority-ordered pipeline:
 *
 * <ol>
 *   <li>CLI argument</li>
 *   <li>Environment variables {@code CATALINA_HOME} / {@code CATALINA_BASE}</li>
 *   <li>systemd override files ({@code /etc/sysconfig/tomcat}, {@code /etc/default/tomcat})</li>
 *   <li>Well-known installation paths</li>
 *   <li>Running process detection via {@code /proc}</li>
 * </ol>
 *
 * <p>{@code CATALINA_BASE} falls back to {@code CATALINA_HOME} when no source
 * provides it explicitly.
 *
 * <p>Use {@link #create(Path, Path)} for production. The package-private
 * constructor accepts injected collaborators for unit testing.
 */
public class CatalinaDiscovery {

    private static final List<Path> DEFAULT_SYSTEMD_FILES = List.of(
        Path.of("/etc/sysconfig/tomcat"),
        Path.of("/etc/default/tomcat")
    );

    private final Path cliHome;
    private final Path cliBase;
    private final EnvironmentSource env;
    private final List<Path> systemdConfigFiles;
    private final WellKnownPaths wellKnownPaths;
    private final ProcessDetector processDetector;

    CatalinaDiscovery(
            Path cliHome,
            Path cliBase,
            EnvironmentSource env,
            List<Path> systemdConfigFiles,
            WellKnownPaths wellKnownPaths,
            ProcessDetector processDetector) {
        this.cliHome = cliHome;
        this.cliBase = cliBase;
        this.env = env;
        this.systemdConfigFiles = systemdConfigFiles;
        this.wellKnownPaths = wellKnownPaths;
        this.processDetector = processDetector;
    }

    /**
     * Creates a production instance reading from the real environment and
     * filesystem.
     *
     * @param cliHome  value of {@code --catalina-home} CLI option, or {@code null}
     * @param cliBase  value of {@code --catalina-base} CLI option, or {@code null}
     */
    public static CatalinaDiscovery create(Path cliHome, Path cliBase) {
        return new CatalinaDiscovery(
            cliHome,
            cliBase,
            System::getenv,
            DEFAULT_SYSTEMD_FILES,
            new WellKnownPaths(Path.of("/")),
            new ProcessDetector(Path.of("/proc"))
        );
    }

    /**
     * Runs the full discovery pipeline and returns the resolved paths.
     * Fields in the result may be {@code null} when no source provided them.
     */
    public Result discover() {
        Optional<ProcessDetector.Result> procResult = processDetector.detect();
        Path home = resolveHome(procResult);
        Path base = resolveBase(home, procResult);
        Integer pid = procResult.map(ProcessDetector.Result::getPid).orElse(null);
        return new Result(home, base, pid);
    }

    private Path resolveHome(Optional<ProcessDetector.Result> proc) {
        if (isDirectory(cliHome)) {
            return cliHome;
        }
        Path fromEnv = pathFromEnv("CATALINA_HOME");
        if (fromEnv != null) {
            return fromEnv;
        }
        Path fromSystemd = pathFromSystemd("CATALINA_HOME");
        if (fromSystemd != null) {
            return fromSystemd;
        }
        Optional<Path> fromWellKnown = wellKnownPaths.find();
        if (fromWellKnown.isPresent()) {
            return fromWellKnown.get();
        }
        return proc.map(ProcessDetector.Result::getCatalinaHome).orElse(null);
    }

    private Path resolveBase(Path resolvedHome, Optional<ProcessDetector.Result> proc) {
        if (isDirectory(cliBase)) {
            return cliBase;
        }
        Path fromEnv = pathFromEnv("CATALINA_BASE");
        if (fromEnv != null) {
            return fromEnv;
        }
        Path fromSystemd = pathFromSystemd("CATALINA_BASE");
        if (fromSystemd != null) {
            return fromSystemd;
        }
        Path fromProc = proc.map(ProcessDetector.Result::getCatalinaBase).orElse(null);
        if (fromProc != null) {
            return fromProc;
        }
        // Default: same directory as HOME
        return resolvedHome;
    }

    private static boolean isDirectory(Path p) {
        return p != null && Files.isDirectory(p);
    }

    private Path pathFromEnv(String variable) {
        String value = env.getenv(variable);
        if (value == null || value.isBlank()) {
            return null;
        }
        Path p = Path.of(value);
        return Files.isDirectory(p) ? p : null;
    }

    private Path pathFromSystemd(String key) {
        for (Path configFile : systemdConfigFiles) {
            if (!Files.isReadable(configFile)) {
                continue;
            }
            Map<String, String> props = new SystemdConfigParser(configFile).parse();
            String value = props.get(key);
            if (value != null && !value.isBlank()) {
                Path p = Path.of(value);
                if (Files.isDirectory(p)) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * Outcome of the discovery pipeline.
     *
     * <p>Any field may be {@code null} when no source provided a value for it.
     * {@code catalinaBase} is {@code null} only when {@code catalinaHome} is also
     * {@code null}; otherwise it defaults to {@code catalinaHome}.
     */
    public static final class Result {
        private final Path catalinaHome;
        private final Path catalinaBase;
        private final Integer pid;

        Result(Path catalinaHome, Path catalinaBase, Integer pid) {
            this.catalinaHome = catalinaHome;
            this.catalinaBase = catalinaBase;
            this.pid = pid;
        }

        public Path getCatalinaHome() { return catalinaHome; }
        public Path getCatalinaBase() { return catalinaBase; }
        public Integer getPid()       { return pid; }
    }
}
