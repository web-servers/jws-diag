package org.jboss.jws.diag.summary.discovery;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the JBoss Web Server (JWS) version.
 *
 * <p>Two strategies, tried in order:
 * <ol>
 *   <li>Extract the numeric JWS major version from the installation path
 *       (e.g. {@code /opt/rh/jws6/root/...} &rarr; {@code "6"}).</li>
 *   <li>Scan {@code CATALINA_HOME/lib/} for a {@code jws-*.jar} and parse
 *       the version from the filename (e.g. {@code jws-6.1.0.jar} &rarr;
 *       {@code "6.1.0"}).</li>
 * </ol>
 *
 * <p>Returns {@code null} when neither strategy succeeds or {@code catalinaHome}
 * is null.
 */
class JwsVersionDetector {

    private static final Pattern JWS_PATH_PATTERN = Pattern.compile("/jws(\\d+)/");

    private final Path catalinaHome;

    JwsVersionDetector(Path catalinaHome) {
        this.catalinaHome = catalinaHome;
    }

    String detect() {
        if (catalinaHome == null) {
            return null;
        }

        Matcher m = JWS_PATH_PATTERN.matcher(catalinaHome.toString());
        if (m.find()) {
            return m.group(1);
        }

        Path lib = catalinaHome.resolve("lib");
        if (Files.isDirectory(lib)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(lib, "jws-*.jar")) {
                for (Path jar : stream) {
                    String name = jar.getFileName().toString();
                    String ver = name.replaceFirst("^jws-", "").replaceFirst("\\.jar$", "");
                    if (!ver.isEmpty()) {
                        return ver;
                    }
                }
            } catch (IOException ignored) {
            }
        }

        return null;
    }
}
