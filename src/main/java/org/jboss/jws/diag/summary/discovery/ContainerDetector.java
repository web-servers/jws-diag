package org.jboss.jws.diag.summary.discovery;

import org.jboss.jws.diag.summary.model.ContainerInfo;
import org.jboss.jws.diag.summary.model.ContainerType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects the container runtime in which the Tomcat process is running.
 *
 * <p>Priority order (highest to lowest):
 * <ol>
 *   <li>{@code /.dockerenv} present &rarr; Docker</li>
 *   <li>{@code /run/.containerenv} present &rarr; Podman</li>
 *   <li>{@code KUBERNETES_SERVICE_HOST} env var set &rarr; Kubernetes</li>
 *   <li>{@code /proc/1/cgroup} contains "docker" or "containerd" &rarr; Docker (cgroup)</li>
 *   <li>None matched &rarr; {@link ContainerType#BARE_METAL}</li>
 * </ol>
 */
class ContainerDetector {

    private final Path fsRoot;
    private final EnvironmentSource env;

    static ContainerDetector create() {
        return new ContainerDetector(Path.of("/"), System::getenv);
    }

    ContainerDetector(Path fsRoot, EnvironmentSource env) {
        this.fsRoot = fsRoot;
        this.env = env;
    }

    ContainerInfo detect() {
        if (Files.exists(fsRoot.resolve(".dockerenv"))) {
            return result(ContainerType.DOCKER, "/.dockerenv");
        }

        if (Files.exists(fsRoot.resolve("run/.containerenv"))) {
            return result(ContainerType.PODMAN, "/run/.containerenv");
        }

        if (env.getenv("KUBERNETES_SERVICE_HOST") != null) {
            return result(ContainerType.KUBERNETES, "KUBERNETES_SERVICE_HOST");
        }

        Path cgroup = fsRoot.resolve("proc/1/cgroup");
        if (Files.exists(cgroup)) {
            try {
                String content = Files.readString(cgroup);
                if (content.contains("docker") || content.contains("containerd")) {
                    return result(ContainerType.DOCKER, "/proc/1/cgroup");
                }
            } catch (IOException ignored) {
            }
        }

        return result(ContainerType.BARE_METAL, null);
    }

    private static ContainerInfo result(ContainerType type, String detectedVia) {
        return ContainerInfo.builder().type(type).detectedVia(detectedVia).build();
    }
}
