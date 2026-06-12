package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.OsInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Detects OS name, version, and architecture.
 *
 * <p>Reads {@code /etc/os-release} first; falls back to system properties
 * ({@code os.name}, {@code os.version}, {@code os.arch}) when the file is absent.
 */
class OsDetector {

    @FunctionalInterface
    interface PropertySource {
        String getProperty(String name);
    }

    private final Path fsRoot;
    private final PropertySource properties;

    static OsDetector create() {
        return new OsDetector(Path.of("/"), System::getProperty);
    }

    OsDetector(Path fsRoot, PropertySource properties) {
        this.fsRoot = fsRoot;
        this.properties = properties;
    }

    OsInfo detect() {
        Path osRelease = fsRoot.resolve("etc/os-release");
        if (Files.exists(osRelease)) {
            Map<String, String> props = new SystemdConfigParser(osRelease).parse();
            String name    = props.get("NAME");
            String version = props.get("VERSION_ID");
            String arch    = properties.getProperty("os.arch");
            if (name != null || version != null) {
                return OsInfo.builder().name(name).version(version).arch(arch).build();
            }
        }
        return OsInfo.builder()
                .name(properties.getProperty("os.name"))
                .version(properties.getProperty("os.version"))
                .arch(properties.getProperty("os.arch"))
                .build();
    }
}
