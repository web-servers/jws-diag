package org.jboss.jws.diag.summary.discovery;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Probes well-known filesystem locations for a Tomcat / JWS installation.
 *
 * <p>Search order:
 * <ol>
 *   <li>JWS RPM paths: {@code <fsRoot>/opt/rh/jws&#42;/root/usr/share/tomcat} (newest version first)</li>
 *   <li>Common upstream paths: {@code usr/share/tomcat}, {@code opt/tomcat}</li>
 * </ol>
 *
 * <p>A candidate is accepted only when it contains both
 * {@code lib/catalina.jar} and {@code conf/server.xml}.
 */
class WellKnownPaths {

    private static final List<String> STATIC_CANDIDATES = List.of(
        "usr/share/tomcat",
        "opt/tomcat"
    );

    private final Path fsRoot;

    WellKnownPaths(Path fsRoot) {
        this.fsRoot = fsRoot;
    }

    Optional<Path> find() {
        Optional<Path> fromJws = findJwsPath();
        if (fromJws.isPresent()) {
            return fromJws;
        }
        for (String relative : STATIC_CANDIDATES) {
            Path candidate = fsRoot.resolve(relative);
            if (isValidTomcatHome(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    private Optional<Path> findJwsPath() {
        Path jwsParent = fsRoot.resolve("opt/rh");
        if (!Files.isDirectory(jwsParent)) {
            return Optional.empty();
        }
        List<Path> candidates = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(jwsParent, "jws*")) {
            for (Path entry : stream) {
                Path candidate = entry.resolve("root/usr/share/tomcat");
                if (isValidTomcatHome(candidate)) {
                    candidates.add(candidate);
                }
            }
        } catch (IOException e) {
            return Optional.empty();
        }
        // Prefer newest version (jws6 > jws5) by numeric suffix descending.
        candidates.sort(Comparator.comparing(
            p -> p.getParent().getParent().getParent().getParent().getFileName().toString(),
            Comparator.comparingInt(WellKnownPaths::jwsVersion).reversed()
        ));
        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
    }

    private static int jwsVersion(String dirName) {
        String digits = dirName.replaceAll("[^0-9]", "");
        try {
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    static boolean isValidTomcatHome(Path path) {
        return Files.isDirectory(path)
            && Files.isRegularFile(path.resolve("lib/catalina.jar"))
            && Files.isRegularFile(path.resolve("conf/server.xml"));
    }
}
