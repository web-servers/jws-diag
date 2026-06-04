package org.jboss.jws.diag.summary.discovery;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Detects a running Tomcat process by scanning {@code /proc/&#42;/cmdline}.
 *
 * <p>Identifies Tomcat by the presence of
 * {@code org.apache.catalina.startup.Bootstrap} in the command line and
 * extracts {@code -Dcatalina.home} and {@code -Dcatalina.base} JVM flags.
 * Entries that cannot be read (e.g. due to permissions) are silently skipped.
 */
class ProcessDetector {

    private static final String BOOTSTRAP_CLASS = "org.apache.catalina.startup.Bootstrap";

    private final Path procRoot;

    ProcessDetector(Path procRoot) {
        this.procRoot = procRoot;
    }

    /**
     * Scans the proc filesystem and returns the first Tomcat process found,
     * or {@link Optional#empty()} when none is running or the proc filesystem
     * is unavailable.
     */
    Optional<Result> detect() {
        if (!Files.isDirectory(procRoot)) {
            return Optional.empty();
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(procRoot, this::isPidDirectory)) {
            for (Path pidDir : stream) {
                Optional<Result> result = inspectProcess(pidDir);
                if (result.isPresent()) {
                    return result;
                }
            }
        } catch (IOException e) {
            // Cannot iterate /proc — not a fatal error
        }
        return Optional.empty();
    }

    private boolean isPidDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            return false;
        }
        String name = path.getFileName().toString();
        return !name.isEmpty() && name.chars().allMatch(Character::isDigit);
    }

    private Optional<Result> inspectProcess(Path pidDir) {
        Path cmdlineFile = pidDir.resolve("cmdline");
        if (!Files.isReadable(cmdlineFile)) {
            return Optional.empty();
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(cmdlineFile);
        } catch (IOException e) {
            return Optional.empty();
        }
        List<String> args = splitOnNullByte(bytes);
        boolean isTomcat = args.stream().anyMatch(a -> a.contains(BOOTSTRAP_CLASS));
        if (!isTomcat) {
            return Optional.empty();
        }
        int pid = Integer.parseInt(pidDir.getFileName().toString());
        Path home = extractFlag(args, "catalina.home");
        Path base = extractFlag(args, "catalina.base");
        return Optional.of(new Result(pid, home, base));
    }

    private static List<String> splitOnNullByte(byte[] bytes) {
        List<String> args = new ArrayList<>();
        int start = 0;
        for (int i = 0; i <= bytes.length; i++) {
            if (i == bytes.length || bytes[i] == 0) {
                if (i > start) {
                    args.add(new String(bytes, start, i - start));
                }
                start = i + 1;
            }
        }
        return args;
    }

    private static Path extractFlag(List<String> args, String property) {
        String prefix = "-D" + property + "=";
        return args.stream()
            .filter(a -> a.startsWith(prefix))
            .map(a -> Path.of(a.substring(prefix.length())))
            .filter(Files::isDirectory)
            .findFirst()
            .orElse(null);
    }

    /**
     * Result of a successful Tomcat process detection.
     */
    static final class Result {
        private final int pid;
        private final Path catalinaHome;
        private final Path catalinaBase;

        Result(int pid, Path catalinaHome, Path catalinaBase) {
            this.pid = pid;
            this.catalinaHome = catalinaHome;
            this.catalinaBase = catalinaBase;
        }

        int getPid() { return pid; }
        Path getCatalinaHome() { return catalinaHome; }
        Path getCatalinaBase() { return catalinaBase; }
    }
}
