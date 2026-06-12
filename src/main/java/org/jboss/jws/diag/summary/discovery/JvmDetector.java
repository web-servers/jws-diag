package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.JvmInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects JVM version, vendor, home directory, and JVM arguments.
 *
 * <p>Version, vendor, and home come from system properties. JVM arguments are read
 * from {@code /proc/<pid>/cmdline} when a Tomcat PID is available; sensitive
 * {@code -D} flags (those containing "password", "secret", "credential", or "keypass")
 * are redacted before being stored.
 */
class JvmDetector {

    @FunctionalInterface
    interface PropertySource {
        String getProperty(String name);
    }

    private final PropertySource properties;
    private final Path procRoot;
    private final Integer pid;

    static JvmDetector create(Integer pid) {
        return new JvmDetector(System::getProperty, Path.of("/proc"), pid);
    }

    JvmDetector(PropertySource properties, Path procRoot, Integer pid) {
        this.properties = properties;
        this.procRoot = procRoot;
        this.pid = pid;
    }

    JvmInfo detect() {
        String version = properties.getProperty("java.version");
        String vendor  = properties.getProperty("java.vendor");
        String homeStr = properties.getProperty("java.home");
        Path javaHome  = homeStr != null ? Path.of(homeStr) : null;

        List<String> jvmArgs = pid != null ? readJvmArgs(pid) : List.of();

        return JvmInfo.builder()
                .version(version)
                .vendor(vendor)
                .javaHome(javaHome)
                .jvmArgs(jvmArgs)
                .build();
    }

    private List<String> readJvmArgs(int pid) {
        Path cmdline = procRoot.resolve(String.valueOf(pid)).resolve("cmdline");
        try {
            byte[] bytes = Files.readAllBytes(cmdline);
            List<String> args = splitOnNullByte(bytes);
            List<String> result = new ArrayList<>();
            for (String arg : args) {
                if ((arg.startsWith("-X") || arg.startsWith("-D")) && !isSensitive(arg)) {
                    result.add(arg);
                }
            }
            return List.copyOf(result);
        } catch (IOException e) {
            return List.of();
        }
    }

    private static List<String> splitOnNullByte(byte[] bytes) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                if (i > start) {
                    parts.add(new String(bytes, start, i - start));
                }
                start = i + 1;
            }
        }
        if (start < bytes.length) {
            parts.add(new String(bytes, start, bytes.length - start));
        }
        return parts;
    }

    private static boolean isSensitive(String arg) {
        String lower = arg.toLowerCase();
        return lower.contains("password") || lower.contains("secret")
                || lower.contains("credential") || lower.contains("keypass");
    }
}
