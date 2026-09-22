package org.jboss.jws.diag.instances;

import org.jboss.jws.diag.instances.model.TomcatInstance;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scans {@code /proc} and returns every running Tomcat/JWS instance.
 *
 * <p>Unlike the discovery pipeline's {@code ProcessDetector} — which stops at
 * the first match — this scanner collects <em>all</em> Tomcat processes, which
 * is the key requirement for multi-instance reporting.
 *
 * <p>Entries that cannot be read (permission errors, races with process exit)
 * are silently skipped so the scan always produces a best-effort result.
 */
public final class InstanceScanner {

    private static final String BOOTSTRAP_CLASS = "org.apache.catalina.startup.Bootstrap";
    private static final Path DEFAULT_PROC = Path.of("/proc");

    private final Path procRoot;

    public InstanceScanner() {
        this(DEFAULT_PROC);
    }

    /** Scans an alternative /proc root. Used by tests and by rules that read /proc themselves. */
    public InstanceScanner(Path procRoot) {
        this.procRoot = procRoot;
    }

    /**
     * Returns all discovered Tomcat instances, sorted by PID ascending.
     * Returns an empty list if none are found or {@code /proc} is unavailable.
     */
    public List<TomcatInstance> scan() {
        if (!Files.isDirectory(procRoot)) {
            return Collections.emptyList();
        }
        List<TomcatInstance> results = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(procRoot, this::isPidDir)) {
            for (Path pidDir : stream) {
                TomcatInstance instance = inspect(pidDir);
                if (instance != null) {
                    results.add(instance);
                }
            }
        } catch (IOException ignored) {
            // Cannot iterate /proc — return whatever was collected
        }
        results.sort((a, b) -> Integer.compare(a.getPid(), b.getPid()));
        return Collections.unmodifiableList(results);
    }

    private boolean isPidDir(Path path) {
        if (!Files.isDirectory(path)) return false;
        String name = path.getFileName().toString();
        return !name.isEmpty() && name.chars().allMatch(Character::isDigit);
    }

    private TomcatInstance inspect(Path pidDir) {
        Path cmdlineFile = pidDir.resolve("cmdline");
        if (!Files.isReadable(cmdlineFile)) return null;
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(cmdlineFile);
        } catch (IOException e) {
            return null;
        }
        List<String> args = splitNullByte(bytes);
        if (args.stream().noneMatch(a -> a.contains(BOOTSTRAP_CLASS))) return null;

        int pid = Integer.parseInt(pidDir.getFileName().toString());
        Path home = extractPath(args, "catalina.home");
        Path base = extractPath(args, "catalina.base");
        if (base == null) base = home;
        String catalinaOut = readEnvironmentValue(pidDir.resolve("environ"), "CATALINA_OUT");
        return new TomcatInstance(pid, home, base, catalinaOut);
    }

    // /proc/<pid>/environ is normally readable only by the process owner. When it
    // cannot be read the value is unknown, which callers treat the same as unset.
    private static String readEnvironmentValue(Path environFile, String name) {
        if (!Files.isReadable(environFile)) return null;
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(environFile);
        } catch (IOException e) {
            return null;
        }
        String prefix = name + "=";
        for (String entry : splitNullByte(bytes)) {
            if (entry.startsWith(prefix)) {
                return entry.substring(prefix.length());
            }
        }
        return null;
    }

    private static List<String> splitNullByte(byte[] bytes) {
        List<String> args = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= bytes.length; i++) {
            if (i == bytes.length || bytes[i] == 0) {
                if (i > start) args.add(new String(bytes, start, i - start));
                start = i + 1;
            }
        }
        return args;
    }

    private static Path extractPath(List<String> args, String property) {
        String prefix = "-D" + property + "=";
        return args.stream()
                .filter(a -> a.startsWith(prefix))
                .map(a -> Path.of(a.substring(prefix.length())))
                .findFirst()
                .orElse(null);
    }
}
